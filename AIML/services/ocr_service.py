from models.schemas import ExtractedEntity, BoundingBox

class OCRService:
    def __init__(self):
        # TODO: Initialize PaddleOCR instance here
        # self.ocr = PaddleOCR(use_angle_cls=True, lang='en')
        pass

    def process_image(self, file_bytes: bytes) -> dict:
        """
        Processes the image bytes to extract text and bounding boxes.
        Currently returns mock data until PaddleOCR is integrated.
        """
        # In the future, we will convert file_bytes to an image array
        # and pass it to self.ocr.ocr()
        
        mock_entity = ExtractedEntity(
            entity_type="AADHAAR",
            value="1234-5678-9012",
            confidence=0.98,
            bounding_box=BoundingBox(min_x=10.5, min_y=20.0, max_x=150.5, max_y=35.0)
        )
        
        return {
            "extracted_text": "This is a mock extracted text from the document. Suspect Aadhaar is 1234-5678-9012.",
            "entities": [mock_entity]
        }

# Global instance to be used by routers
ocr_service = OCRService()
