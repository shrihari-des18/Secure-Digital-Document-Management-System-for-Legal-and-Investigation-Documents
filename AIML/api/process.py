import cv2
import uuid
from fastapi import APIRouter, UploadFile, File, HTTPException
from models.schemas import DocumentProcessResponse
from services.ocr_service import ocr_service
from services.classifier_service import classifier_service
from services.tamper_service import tamper_service

router = APIRouter()


@router.post("/process-document", response_model=DocumentProcessResponse, tags=["Processing"])
async def process_document(file: UploadFile = File(...)):
    """
    Process an uploaded document (image or PDF).

    Full pipeline per request:
    1. OCR  — PaddleOCR text extraction + pixel-accurate bounding boxes
               (PDF pages are each rendered to an image first)
    2. NER  — regex engine for Aadhaar, phone, BNS/IPC sections (runs inside OCR service)
    3. Classify — keyword heuristics → FIR / CHARGE_SHEET / SEIZURE_MEMO / FORENSIC_REPORT
    4. Tamper   — ELA on the first page image
    """
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file provided")

    file_bytes = await file.read()

    # OCR — returns decoded page images so tamper detection can reuse them
    # without re-decoding the file a second time.
    pages, extracted_text, entities = ocr_service.process_image_raw(file_bytes)

    # Classification runs on the full text across all pages
    document_type = classifier_service.classify_document(extracted_text)

    # ELA tamper detection runs on the first page.
    # Re-encode the already-decoded image as JPEG bytes (ELA needs JPEG compression).
    tamper_result = {"is_tampered": False, "tamper_score": 0.0}
    if pages:
        _, jpg_buf = cv2.imencode(".jpg", pages[0], [int(cv2.IMWRITE_JPEG_QUALITY), 95])
        tamper_result = tamper_service.analyze_image(jpg_buf.tobytes())

    return DocumentProcessResponse(
        document_id=str(uuid.uuid4()),
        status="SUCCESS",
        document_type=document_type,
        is_tampered=tamper_result["is_tampered"],
        tamper_score=tamper_result["tamper_score"],
        extracted_text=extracted_text,
        entities=entities,
    )
