from fastapi import APIRouter, UploadFile, File, HTTPException
from models.schemas import DocumentProcessResponse
from services.ocr_service import ocr_service
from services.classifier_service import classifier_service
import uuid

router = APIRouter()

@router.post("/process-document", response_model=DocumentProcessResponse, tags=["Processing"])
async def process_document(file: UploadFile = File(...)):
    """
    Endpoint to process an uploaded document (FIR, notes, etc.).
    Delegates processing to the OCRService and applies Document Classification.
    """
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file provided")
    
    # Read file bytes
    file_bytes = await file.read()
    
    # Call the OCR service
    processing_result = ocr_service.process_image(file_bytes)
    extracted_text = processing_result["extracted_text"]
    
    # Classify the document based on extracted text
    document_type = classifier_service.classify_document(extracted_text)
    
    return DocumentProcessResponse(
        document_id=str(uuid.uuid4()),
        status="SUCCESS",
        document_type=document_type,
        extracted_text=extracted_text,
        entities=processing_result["entities"]
    )
