import re

class ClassifierService:
    def __init__(self):
        # Keyword based heuristics for document classification
        self.rules = {
            "FIR": [r'\bfirst information report\b', r'\bf.i.r\b', r'\bpolice station\b', r'\binformant\b', r'\baccused\b'],
            "SEIZURE_MEMO": [r'\bseizure memo\b', r'\bpanchnama\b', r'\bseized items\b', r'\bwitnesses\b', r'\brecovery\b'],
            "CHARGE_SHEET": [r'\bcharge sheet\b', r'\bchallan\b', r'\bfinal report\b', r'\bmagistrate\b'],
            "FORENSIC_REPORT": [r'\bforensic science laboratory\b', r'\bfsl\b', r'\bexpert opinion\b', r'\bballistic\b', r'\bchemical analysis\b']
        }

    def classify_document(self, text: str) -> str:
        """
        Classifies the document type based on keyword frequency and regex matching.
        """
        text_lower = text.lower()
        scores = {doc_type: 0 for doc_type in self.rules.keys()}
        
        for doc_type, patterns in self.rules.items():
            for pattern in patterns:
                # Count occurrences of each pattern
                matches = re.findall(pattern, text_lower)
                scores[doc_type] += len(matches)
                
        # Find the document type with the highest score
        best_match = max(scores, key=scores.get)
        
        # If no keywords matched, return UNKNOWN
        if scores[best_match] == 0:
            return "UNKNOWN"
            
        return best_match

classifier_service = ClassifierService()
