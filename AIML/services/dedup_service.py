import hashlib
import io
import numpy as np


def _image_phash(img_bgr: "np.ndarray") -> str:
    """
    Compute a perceptual hash (pHash) for an image using imagehash.
    Returns a hex string. Requires imagehash + Pillow.
    """
    try:
        import imagehash
        from PIL import Image
        import cv2
        rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
        pil = Image.fromarray(rgb)
        return str(imagehash.phash(pil))
    except ImportError:
        raise RuntimeError(
            "imagehash and Pillow are required for image dedup. "
            "Install with: pip install imagehash Pillow"
        )


def _text_fingerprint(text: str) -> str:
    """
    Simple SHA-256 fingerprint of normalised document text.
    Used for exact-duplicate detection on text documents.
    For fuzzy matching, replace with MinHash (datasketch library).
    """
    normalised = " ".join(text.lower().split())
    return hashlib.sha256(normalised.encode("utf-8")).hexdigest()


class DedupService:
    """
    Computes perceptual/content hashes for images and text documents.
    Actual duplicate lookup is delegated to the caller (Spring backend
    holds the index of known hashes).
    """

    def analyze_image(self, img_bgr: "np.ndarray") -> dict:
        """
        Returns a pHash for the image.
        The Spring backend compares this against its stored hash index.
        """
        try:
            phash = _image_phash(img_bgr)
            return {"phash": phash, "method": "phash"}
        except RuntimeError as e:
            return {"phash": None, "method": "unavailable", "error": str(e)}

    def analyze_text(self, text: str) -> dict:
        """
        Returns a SHA-256 fingerprint of the document text.
        """
        return {
            "text_fingerprint": _text_fingerprint(text),
            "method": "sha256_normalised",
        }


dedup_service = DedupService()
