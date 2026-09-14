import re
from typing import List, Dict, Any
from models.schemas import ExtractedEntity, BoundingBox

class NERService:
    def __init__(self):
        # Define regex patterns for PII and Statutory Sections
        self.patterns = {
            "AADHAAR": re.compile(r'\b\d{4}[\s\-]?\d{4}[\s\-]?\d{4}\b'),
            "PHONE_NUMBER": re.compile(r'\b(?:(?:\+?91[\s\-]?)?[6789]\d{9})\b'),
            "BNS_SECTION": re.compile(r'\b(?:BNS|Bharatiya Nyaya Sanhita)[\s\-]*(?:Sec|Section|u/s)?[\s\-]*(\d{1,3}(?:\(\d+\))?[a-zA-Z]?)\b', re.IGNORECASE),
            "IPC_SECTION": re.compile(r'\b(?:IPC|Indian Penal Code)[\s\-]*(?:Sec|Section|u/s)?[\s\-]*(\d{1,3}[a-zA-Z]?)\b', re.IGNORECASE)
        }

    def extract_entities(self, text: str, bounding_box: BoundingBox = None) -> List[ExtractedEntity]:
        """
        Extracts entities from a given string using Regex patterns.
        In a real scenario, this would be applied line-by-line or block-by-block from OCR results.
        """
        entities = []
        
        for entity_type, pattern in self.patterns.items():
            for match in pattern.finditer(text):
                matched_value = match.group(0)
                entities.append(ExtractedEntity(
                    entity_type=entity_type,
                    value=matched_value,
                    confidence=1.0,  # Regex matches have high confidence
                    bounding_box=bounding_box
                ))
                
        return entities

ner_service = NERService()
