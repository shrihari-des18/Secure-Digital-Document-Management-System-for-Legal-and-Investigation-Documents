from fastapi import APIRouter, UploadFile, File, HTTPException
from models.schemas import DocumentProcessResponse, ExtractedEntity, BoundingBox
import uuid

router = APIRouter()

@router.post("/process-document", response_model=DocumentProcessResponse, tags=["Processing"])
async def process_document(file: UploadFile = File(...)):
    """
    Endpoint to process an uploaded document (FIR, notes, etc.).
    Currently returns a mock response until the PaddleOCR/NLP pipeline is integrated.
    """
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file provided")
    
    # TODO: Save file temporarily, run PaddleOCR and NER extraction
    
    # Mocking a response for now
    mock_entity = ExtractedEntity(
        entity_type="AADHAAR",
        value="1234-5678-9012",
        confidence=0.98,
        bounding_box=BoundingBox(min_x=10.5, min_y=20.0, max_x=150.5, max_y=35.0)
    )
    
    return DocumentProcessResponse(
        document_id=str(uuid.uuid4()),
        status="SUCCESS",
        extracted_text="This is a mock extracted text from the document. Suspect Aadhaar is 1234-5678-9012.",
        entities=[mock_entity]
    )
