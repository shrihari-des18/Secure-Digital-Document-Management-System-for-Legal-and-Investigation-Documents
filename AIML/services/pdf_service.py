import io
import numpy as np
import cv2


def pdf_to_images(file_bytes: bytes) -> list[np.ndarray]:
    """
    Converts each page of a PDF to an OpenCV BGR image array.
    Returns a list of images, one per page.

    Requires pypdfium2 (pip install pypdfium2). Falls back gracefully
    if the library is not installed — callers should check the return value.
    """
    try:
        import pypdfium2 as pdfium
    except ImportError:
        raise RuntimeError(
            "pypdfium2 is required for PDF support. "
            "Install it with: pip install pypdfium2"
        )

    pdf = pdfium.PdfDocument(file_bytes)
    images: list[np.ndarray] = []

    for page_index in range(len(pdf)):
        page = pdf[page_index]
        # Render at 200 DPI (scale=200/72 ≈ 2.78) for good OCR quality
        # without excessive memory use on long documents.
        bitmap = page.render(scale=200 / 72, rotation=0)
        pil_image = bitmap.to_pil()
        # Convert PIL RGBA/RGB → OpenCV BGR
        img_array = np.array(pil_image.convert("RGB"))
        bgr = cv2.cvtColor(img_array, cv2.COLOR_RGB2BGR)
        images.append(bgr)

    pdf.close()
    return images


def is_pdf(file_bytes: bytes) -> bool:
    """Detect PDF by magic bytes (%PDF-)."""
    return file_bytes[:5] == b"%PDF-"
