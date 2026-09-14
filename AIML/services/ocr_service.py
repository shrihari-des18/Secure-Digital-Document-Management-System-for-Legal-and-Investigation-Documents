import os
import sys

# --- Windows CPU / PaddlePaddle 3.x oneDNN crash workaround ---
# PaddlePaddle 3.3+ on Windows hits a NotImplementedError in the PIR executor
# when oneDNN is enabled: "ConvertPirAttribute2RuntimeAttribute not support
# [pir::ArrayAttribute<pir::DoubleAttribute>]" (onednn_instruction.cc).
# The fix (upstream PR #18340) sets DEFAULT_ENABLE_MKLDNN=False on win32.
# We also set the low-level flag here as a belt-and-suspenders measure.
if sys.platform == "win32":
    os.environ.setdefault("PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT", "0")

import cv2
import numpy as np
from paddleocr import PaddleOCR
from models.schemas import ExtractedEntity, BoundingBox
from services.ner_service import ner_service


class OCRService:
    def __init__(self):
        # Initialize PaddleOCR v3.7+ (PaddleX-based).
        # enable_mkldnn=False is the Python-level knob; the _constants.py patch
        # and the env var above cover the lower-level path on Windows CPU.
        # use_textline_orientation replaces the deprecated use_angle_cls.
        self.ocr = PaddleOCR(
            use_textline_orientation=True,
            lang="en",
            enable_mkldnn=False,
            cpu_threads=4,
        )

    def process_image(self, file_bytes: bytes) -> dict:
        """
        Processes raw image bytes and returns extracted text + NER entities.

        PaddleOCR v3.7 returns a list of OCRResult objects (one per input image).
        Each result is dict-like with keys:
          - rec_polys  : list of N boxes, each [[x1,y1],[x2,y2],[x3,y3],[x4,y4]]
          - rec_texts  : list of N text strings
          - rec_scores : list of N confidence floats
        """
        # Decode bytes → OpenCV image
        np_arr = np.frombuffer(file_bytes, np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

        if img is None:
            raise ValueError("Failed to decode image from uploaded file.")

        # Run OCR — .ocr() is a deprecated alias for .predict(); both work
        results = self.ocr.predict(img)

        full_text: list[str] = []
        all_entities: list[ExtractedEntity] = []

        # results is a list of OCRResult objects, one per input.
        # We always send one image, so results[0] is our result.
        if not results:
            return {"extracted_text": "", "entities": []}

        ocr_result = results[0]  # OCRResult (dict-like)

        rec_polys = ocr_result.get("rec_polys", [])
        rec_texts = ocr_result.get("rec_texts", [])
        rec_scores = ocr_result.get("rec_scores", [])

        for box_coords, text_content, score in zip(rec_polys, rec_texts, rec_scores):
            if not text_content:
                continue

            full_text.append(text_content)

            # box_coords is [[x1,y1],[x2,y2],[x3,y3],[x4,y4]] — may be ndarray
            coords = np.array(box_coords)
            bbox = BoundingBox(
                min_x=float(coords[:, 0].min()),
                min_y=float(coords[:, 1].min()),
                max_x=float(coords[:, 0].max()),
                max_y=float(coords[:, 1].max()),
            )

            line_entities = ner_service.extract_entities(
                text=text_content, bounding_box=bbox
            )
            all_entities.extend(line_entities)

        return {
            "extracted_text": "\n".join(full_text),
            "entities": all_entities,
        }


# Module-level singleton used by the router
ocr_service = OCRService()
