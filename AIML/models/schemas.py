from pydantic import BaseModel, Field
from typing import List, Optional

class BoundingBox(BaseModel):
    min_x: float
    min_y: float
    max_x: float
    max_y: float

class ExtractedEntity(BaseModel):
    entity_type: str = Field(..., description="Type of entity (e.g., AADHAAR, BNS_SECTION, PHONE_NUMBER)")
    value: str = Field(..., description="The extracted text value")
    confidence: float = Field(..., ge=0.0, le=1.0)
    bounding_box: Optional[BoundingBox] = None

class DocumentProcessResponse(BaseModel):
    document_id: str
    status: str = Field(default="SUCCESS")
    extracted_text: str = Field(..., description="Full raw text extracted from the document")
    entities: List[ExtractedEntity] = []
    
class ErrorResponse(BaseModel):
    error: str
    details: Optional[str] = None
