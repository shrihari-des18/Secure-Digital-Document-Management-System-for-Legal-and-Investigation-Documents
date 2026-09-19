"""
POST /ml/v1/analyze

The contract the Spring backend calls. Accepts a signed object URL, a task list,
and optional job metadata. Downloads the file, fans out to the requested services,
and returns structured results.

Supported tasks: "ocr", "classify", "pii", "tamper", "dedup"
  - "pii"      implies "ocr" (NER runs inside the OCR pipeline)
  - "classify" implies "ocr" (classification runs on extracted text)
"""
import uuid
import httpx
import cv2

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional

from services.ocr_service import ocr_service
from services.classifier_service import classifier_service
from services.tamper_service import tamper_service
from services.dedup_service import dedup_service

router = APIRouter()


# ── Request / Response models ─────────────────────────────────────────────────

class AnalyzeRequest(BaseModel):
    job_id: str = Field(..., description="Caller-assigned job ID for tracking")
    evidence_id: str = Field(..., description="Evidence item ID in the Spring backend")
    object_url: str = Field(..., description="Pre-signed URL to download the evidence file")
    tasks: list[str] = Field(
        default=["ocr", "classify", "pii", "tamper"],
        description="Tasks to run: ocr, classify, pii, tamper, dedup",
    )
    callback_url: Optional[str] = Field(
        None,
        description="If provided, results will be POSTed here when complete (async mode)",
    )


class OcrResult(BaseModel):
    text: str
    line_count: int


class PiiEntity(BaseModel):
    type: str
    value: str
    confidence: float
    bounding_box: Optional[dict] = None


class TamperResult(BaseModel):
    is_tampered: bool
    tamper_score: float


class DedupResult(BaseModel):
    phash: Optional[str] = None
    text_fingerprint: Optional[str] = None
    method: str


class AnalyzeResponse(BaseModel):
    job_id: str
    evidence_id: str
    status: str = "SUCCESS"
    tasks_run: list[str]
    ocr: Optional[OcrResult] = None
    classify: Optional[str] = None          # document type label
    pii: Optional[list[PiiEntity]] = None
    tamper: Optional[TamperResult] = None
    dedup: Optional[DedupResult] = None


# ── Helper ────────────────────────────────────────────────────────────────────

async def _download_file(url: str) -> bytes:
    """Download file bytes from a pre-signed URL. 30 s timeout."""
    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.get(url)
        if response.status_code != 200:
            raise HTTPException(
                status_code=502,
                detail=f"Failed to download evidence file: HTTP {response.status_code}",
            )
        return response.content


# ── Endpoint ──────────────────────────────────────────────────────────────────

@router.post("/analyze", response_model=AnalyzeResponse, tags=["Analyze"])
async def analyze(req: AnalyzeRequest):
    """
    Multi-task analysis endpoint for the Spring backend AI Gateway.

    The backend sends a job with a signed object URL and a list of tasks.
    This service downloads the file once and fans out to the requested pipelines.

    Notes:
    - "pii" and "classify" silently enable "ocr" (they both depend on extracted text).
    - "tamper" and "dedup" work on the raw image/page data.
    - The ML service never mutates originals — all results are annotations only.
    """
    tasks = {t.lower() for t in req.tasks}

    # pii and classify require ocr
    if "pii" in tasks or "classify" in tasks:
        tasks.add("ocr")

    # Download the file from the signed URL
    file_bytes = await _download_file(req.object_url)

    # Run OCR once and reuse the decoded pages for other tasks
    pages = []
    extracted_text = ""
    entities = []

    if "ocr" in tasks:
        pages, extracted_text, entities = ocr_service.process_image_raw(file_bytes)

    tasks_run: list[str] = []
    result = AnalyzeResponse(
        job_id=req.job_id,
        evidence_id=req.evidence_id,
        tasks_run=tasks_run,
    )

    # OCR result
    if "ocr" in tasks:
        result.ocr = OcrResult(
            text=extracted_text,
            line_count=len(extracted_text.splitlines()),
        )
        tasks_run.append("ocr")

    # PII / NER — entities already extracted by OCR service
    if "pii" in tasks:
        result.pii = [
            PiiEntity(
                type=e.entity_type,
                value=e.value,
                confidence=e.confidence,
                bounding_box=e.bounding_box.model_dump() if e.bounding_box else None,
            )
            for e in entities
        ]
        tasks_run.append("pii")

    # Document classification
    if "classify" in tasks:
        result.classify = classifier_service.classify_document(extracted_text)
        tasks_run.append("classify")

    # Tamper detection — ELA on first page
    if "tamper" in tasks:
        # If OCR ran we already have page images; otherwise decode now
        if not pages:
            from services.pdf_service import is_pdf, pdf_to_images
            import numpy as np
            if is_pdf(file_bytes):
                pages = pdf_to_images(file_bytes)
            else:
                np_arr = np.frombuffer(file_bytes, np.uint8)
                img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
                if img is not None:
                    pages = [img]

        if pages:
            _, jpg_buf = cv2.imencode(".jpg", pages[0], [int(cv2.IMWRITE_JPEG_QUALITY), 95])
            t = tamper_service.analyze_image(jpg_buf.tobytes())
            result.tamper = TamperResult(
                is_tampered=t["is_tampered"],
                tamper_score=t["tamper_score"],
            )
            tasks_run.append("tamper")

    # Deduplication
    if "dedup" in tasks:
        if pages:
            image_dedup = dedup_service.analyze_image(pages[0])
        else:
            image_dedup = {"phash": None, "method": "no_image"}

        text_dedup = dedup_service.analyze_text(extracted_text) if extracted_text else {}

        result.dedup = DedupResult(
            phash=image_dedup.get("phash"),
            text_fingerprint=text_dedup.get("text_fingerprint"),
            method=image_dedup.get("method", "unknown"),
        )
        tasks_run.append("dedup")

    return result
