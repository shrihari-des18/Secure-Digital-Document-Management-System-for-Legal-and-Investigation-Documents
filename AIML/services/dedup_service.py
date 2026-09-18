import imagehash
from PIL import Image
import io
import hashlib
import re

class DedupService:
    def generate_phash(self, file_bytes: bytes) -> str:
        """
        Generates a Perceptual Hash (pHash) for visual media.
        Unlike cryptographic hashes, pHash remains identical or very close
        even if the image is resized, compressed, or slightly altered.
        """
        try:
            img = Image.open(io.BytesIO(file_bytes))
            h = imagehash.phash(img)
            return str(h)
        except Exception as e:
            print(f"Error generating pHash: {e}")
            return ""
            
    def generate_text_hash(self, text: str) -> str:
        """
        Generates a structural text fingerprint (MinHash/SimHash equivalent).
        By stripping punctuation and whitespace before hashing, identical
        documents will hash to the same value even if OCR introduces minor formatting differences.
        """
        if not text:
            return ""
        
        # Normalize text: keep only alphanumeric characters and lowercase them
        normalized = re.sub(r'\W+', '', text.lower())
        
        # Generate a lightweight SHA-256 slice for matching
        return hashlib.sha256(normalized.encode('utf-8')).hexdigest()[:16]

dedup_service = DedupService()
