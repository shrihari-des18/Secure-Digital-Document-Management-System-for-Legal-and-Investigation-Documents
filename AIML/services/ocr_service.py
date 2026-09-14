from models.schemas import ExtractedEntity, BoundingBox
from services.ner_service import ner_service

class OCRService:
    def __init__(self):
        # TODO: Initialize PaddleOCR instance here
        # self.ocr = PaddleOCR(use_angle_cls=True, lang='en')
        pass

    def process_image(self, file_bytes: bytes) -> dict:
        """
        Processes the image bytes to extract text and bounding boxes.
        Currently returns mock OCR data but passes it through the real NER service.
        """
        # In the future, we will convert file_bytes to an image array
        # and pass it to self.ocr.ocr()
        
        # Mock OCR Output
        mock_extracted_text = "This is a mock extracted text from the document. Suspect Aadhaar is 1234-5678-9012 and Phone number is 9876543210. Registered under BNS Sec 302."
        mock_bbox = BoundingBox(min_x=10.5, min_y=20.0, max_x=150.5, max_y=35.0)
        
        # Apply NER to the extracted text
        entities = ner_service.extract_entities(text=mock_extracted_text, bounding_box=mock_bbox)
        
        return {
            "extracted_text": mock_extracted_text,
            "entities": entities
        }

# Global instance to be used by routers
ocr_service = OCRService()
