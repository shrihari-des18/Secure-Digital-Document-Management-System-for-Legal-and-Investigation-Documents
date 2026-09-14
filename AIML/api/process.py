from fastapi import APIRouter, UploadFile, File, HTTPException
from models.schemas import DocumentProcessResponse
from services.ocr_service import ocr_service
import uuid

router = APIRouter()

@router.post("/process-document", response_model=DocumentProcessResponse, tags=["Processing"])
async def process_document(file: UploadFile = File(...)):
    """
    Endpoint to process an uploaded document (FIR, notes, etc.).
    Delegates processing to the OCRService.
    """
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file provided")
    
    # Read file bytes
    file_bytes = await file.read()
    
    # Call the OCR service
    processing_result = ocr_service.process_image(file_bytes)
    
    return DocumentProcessResponse(
        document_id=str(uuid.uuid4()),
        status="SUCCESS",
        extracted_text=processing_result["extracted_text"],
        entities=processing_result["entities"]
    )
