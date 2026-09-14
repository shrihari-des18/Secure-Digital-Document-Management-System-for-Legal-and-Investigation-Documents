import os
import sys

# Windows CPU / PaddlePaddle 3.x oneDNN crash workaround (must be before paddle import)
if sys.platform == "win32":
    os.environ.setdefault("PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT", "0")

import cv2
import numpy as np
from paddleocr import PaddleOCR


def test_ocr():
    print("Initializing OCR...")
    ocr = PaddleOCR(
        use_textline_orientation=True,
        lang="en",
        enable_mkldnn=False,
        cpu_threads=4,
    )

    # Build a simple test image with white text on black background
    print("Creating test image...")
    img = np.zeros((60, 300, 3), dtype=np.uint8)
    cv2.putText(img, "Aadhaar 1234 5678 9012", (5, 40),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

    print("Running OCR...")
    results = ocr.predict(img)

    if not results:
        print("No results returned.")
        return

    ocr_result = results[0]
    rec_texts = ocr_result.get("rec_texts", [])
    rec_scores = ocr_result.get("rec_scores", [])
    rec_polys = ocr_result.get("rec_polys", [])

    print(f"Detected {len(rec_texts)} text line(s):")
    for text, score, box in zip(rec_texts, rec_scores, rec_polys):
        print(f"  [{score:.3f}] {text!r}  box={np.array(box).tolist()}")

    print("Done.")


if __name__ == "__main__":
    test_ocr()
