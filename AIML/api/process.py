import cv2
import uuid
from fastapi import APIRouter, UploadFile, File, HTTPException
from models.schemas import DocumentProcessResponse
from services.ocr_service import ocr_service
from services.classifier_service import classifier_service
from services.tamper_service import tamper_service
from services.dedup_service import dedup_service

router = APIRouter()


@router.post("/process-document", response_model=DocumentProcessResponse, tags=["Processing"])
async def process_document(file: UploadFile = File(...)):
    """
    Process an uploaded document (image or PDF).

    Full pipeline per request:
    1. OCR  — PaddleOCR text extraction + pixel-accurate bounding boxes
    2. NER  — regex engine for Aadhaar, phone, BNS/IPC sections
    3. Classify — keyword heuristics → FIR / CHARGE_SHEET / SEIZURE_MEMO
    4. Tamper   — ELA on the first page image + EXIF anomaly detection
    5. Dedup    — pHash (visual hashing) + text structural hashing
    """
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file provided")

    file_bytes = await file.read()

    # OCR — returns decoded page images so tamper detection can reuse them
    pages, extracted_text, entities = ocr_service.process_image_raw(file_bytes)

    # Classification runs on the full text across all pages
    document_type = classifier_service.classify_document(extracted_text)
    
    # Generate structural text hash for deduplication
    text_hash = dedup_service.generate_text_hash(extracted_text)
    
    # Check EXIF metadata for the original file upload
    exif_result = tamper_service.check_exif(file_bytes)

    # ELA tamper detection and pHash run on the first page image
    tamper_result = {"is_tampered": False, "tamper_score": 0.0}
    phash_val = ""
    if pages:
        # Re-encode the already-decoded image as JPEG bytes
        _, jpg_buf = cv2.imencode(".jpg", pages[0], [int(cv2.IMWRITE_JPEG_QUALITY), 95])
        page_bytes = jpg_buf.tobytes()
        
        tamper_result = tamper_service.analyze_image(page_bytes)
        phash_val = dedup_service.generate_phash(page_bytes)

    return DocumentProcessResponse(
        document_id=str(uuid.uuid4()),
        status="SUCCESS",
        document_type=document_type,
        is_tampered=tamper_result["is_tampered"],
        tamper_score=tamper_result["tamper_score"],
        exif_suspicious=exif_result.get("exif_suspicious", False),
        phash=phash_val,
        text_hash=text_hash,
        extracted_text=extracted_text,
        entities=entities,
    )
