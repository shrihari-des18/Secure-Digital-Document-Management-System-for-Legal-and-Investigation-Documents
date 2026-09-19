import os
import sys

# Windows CPU / PaddlePaddle 3.x oneDNN crash workaround (must precede paddle import)
# See: github.com/PaddlePaddle/PaddleOCR/pull/18340
if sys.platform == "win32":
    os.environ.setdefault("PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT", "0")

import cv2
import numpy as np
from paddleocr import PaddleOCR
from models.schemas import ExtractedEntity, BoundingBox
from services.ner_service import ner_service
from services.pdf_service import is_pdf, pdf_to_images


class OCRService:
    def __init__(self):
        # enable_mkldnn=False + the env var above are both required on Windows
        # CPU with PaddlePaddle 3.x to prevent the PIR/oneDNN crash.
        # use_textline_orientation replaces the deprecated use_angle_cls in v3.7+.
        self.ocr = PaddleOCR(
            use_textline_orientation=True,
            lang="en",
            enable_mkldnn=False,
            cpu_threads=4,
        )

    def _run_ocr_on_image(self, img: np.ndarray) -> tuple[list[str], list[ExtractedEntity]]:
        """
        Runs PaddleOCR on a single BGR image array.
        Returns (text_lines, entities).
        """
        results = self.ocr.predict(img)
        if not results:
            return [], []

        ocr_result = results[0]
        rec_polys = ocr_result.get("rec_polys", [])
        rec_texts = ocr_result.get("rec_texts", [])
        rec_scores = ocr_result.get("rec_scores", [])

        text_lines: list[str] = []
        entities: list[ExtractedEntity] = []

        for box_coords, text_content, score in zip(rec_polys, rec_texts, rec_scores):
            if not text_content:
                continue
            text_lines.append(text_content)

            coords = np.array(box_coords)
            bbox = BoundingBox(
                min_x=float(coords[:, 0].min()),
                min_y=float(coords[:, 1].min()),
                max_x=float(coords[:, 0].max()),
                max_y=float(coords[:, 1].max()),
            )
            entities.extend(ner_service.extract_entities(text=text_content, bounding_box=bbox))

        return text_lines, entities

    def process_image(self, file_bytes: bytes) -> dict:
        """
        Accepts raw file bytes (image or PDF).
        For PDFs, each page is processed independently and results are concatenated.
        Returns {"extracted_text": str, "entities": list[ExtractedEntity]}.
        """
        if is_pdf(file_bytes):
            pages = pdf_to_images(file_bytes)
        else:
            np_arr = np.frombuffer(file_bytes, np.uint8)
            img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
            if img is None:
                raise ValueError("Failed to decode image from uploaded file.")
            pages = [img]

        all_text: list[str] = []
        all_entities: list[ExtractedEntity] = []

        for page_img in pages:
            text_lines, entities = self._run_ocr_on_image(page_img)
            all_text.extend(text_lines)
            all_entities.extend(entities)

        return {
            "extracted_text": "\n".join(all_text),
            "entities": all_entities,
        }

    def process_image_raw(self, file_bytes: bytes) -> tuple[list[np.ndarray], str, list[ExtractedEntity]]:
        """
        Like process_image() but also returns the decoded page images,
        needed by the analyze endpoint for tamper detection and dedup.
        Returns (pages, extracted_text, entities).
        """
        if is_pdf(file_bytes):
            pages = pdf_to_images(file_bytes)
        else:
            np_arr = np.frombuffer(file_bytes, np.uint8)
            img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
            if img is None:
                raise ValueError("Failed to decode image from uploaded file.")
            pages = [img]

        all_text: list[str] = []
        all_entities: list[ExtractedEntity] = []

        for page_img in pages:
            text_lines, entities = self._run_ocr_on_image(page_img)
            all_text.extend(text_lines)
            all_entities.extend(entities)

        return pages, "\n".join(all_text), all_entities


# Module-level singleton
ocr_service = OCRService()
