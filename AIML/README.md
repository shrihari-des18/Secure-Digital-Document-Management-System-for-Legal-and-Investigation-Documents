# SIH26190 - AI/ML Python Microservice

## Overview
This is the isolated AI/ML microservice built for **SIH26190: Secure Digital Evidence & Legal Document Management System**. It acts as an asynchronous worker to offload heavy computer vision and NLP tasks from the core Spring Boot backend.

**Primary Goal:** To process uploaded evidentiary documents (FIRs, notes) and automatically extract Sensitive PII (Personally Identifiable Information) and Statutory Legal Sections along with their exact pixel-level bounding boxes for dynamic front-end redaction.

---

## 🏗️ Architecture & Modules

### 1. OCR Ingestion Pipeline (`services/ocr_service.py`)
- **Engine:** `PaddleOCR` (v3.7+)
- **Process:** Receives raw byte streams from the API, decodes them into an OpenCV `numpy` array, and extracts text blocks along with their `[x1, y1, x2, y2, x3, y3, x4, y4]` bounding box coordinates.
- **Note to LLMs:** PaddleOCR v3.7+ has drastically changed its initialization and output structure. `enable_mkldnn=False` is required on Windows CPU to prevent C++ crashes. The output parsing logic must strictly handle v3.7's `predict()` return structure.

### 2. NER & Regex Engine (`services/ner_service.py`)
- **Engine:** Custom Regex Patterns
- **Process:** Scans the text extracted by the OCR pipeline to find specific entities.
- **Supported Entities:**
  - `AADHAAR` / `PAN_CARD`: Indian Identity formats
  - `PHONE_NUMBER`: Indian phone formats
  - `BNS_SECTION` / `IPC_SECTION`: Criminal code sections
  - `FIR_NUMBER`: Crime registration numbers
  - `VEHICLE_REG`: Indian vehicle plates
  - `BANK_IFSC`: Banking codes
- **Correlation:** Associates the regex-matched text with the bounding box inherited from the OCR pipeline, formatting it as `[min_x, min_y, max_x, max_y]`.

### 3. Document Classification (`services/classifier_service.py`)
- **Engine:** Keyword Frequency & Heuristics
- **Process:** Analyzes the extracted text to classify the document type (e.g., `FIR`, `SEIZURE_MEMO`, `CHARGE_SHEET`).

### 4. Image Tamper Detection (`services/tamper_service.py`)
- **Engine:** Error Level Analysis (ELA) & EXIF Anomaly Detection (using OpenCV & Pillow)
- **Process:** Identifies digital manipulation by analyzing compression artifacts and inspecting embedded EXIF tags for Photoshop/GIMP signatures. Returns an `is_tampered` boolean flag, `tamper_score`, and `exif_suspicious` flag.

### 5. Media Deduplication Engine (`services/dedup_service.py`)
- **Engine:** Discrete Cosine Transform (pHash) & MinHash Equivalents
- **Process:** Generates a 64-bit visual perceptual hash (`phash`) for images and a structural normalized string hash (`text_hash`) for OCR text. This enables instant cross-case duplicate detection even if images are resized or text has minor OCR errors.

---

## 📡 API Contract (FastAPI)

### `POST /api/v1/process-document`
Accepts a `multipart/form-data` file upload and returns the extracted entities, text, and forensic analysis.

**Response Schema (`DocumentProcessResponse`):**
```json
{
  "document_id": "uuid-string",
  "status": "SUCCESS",
  "document_type": "FIR",
  "is_tampered": false,
  "tamper_score": 1.25,
  "exif_suspicious": false,
  "phash": "a5c7f8a1e64b82d0",
  "text_hash": "4a2b9f87c9d01f22",
  "extracted_text": "Full raw text extracted from the document...",
  "entities": [
    {
      "entity_type": "AADHAAR",
      "value": "1234-5678-9012",
      "confidence": 0.98,
      "bounding_box": {
        "min_x": 10.5,
        "min_y": 20.0,
        "max_x": 150.5,
        "max_y": 35.0
      }
    }
  ]
}
```

---

## 🚀 Quick Start (Automated Scripts)

To instantly start the API without manually setting up Python environments, simply double-click or execute the provided run scripts. They will automatically create the virtual environment, install dependencies, and start the server.

- **Windows:** Double-click or run `run.bat`
- **Linux/Mac:** Run `./run.sh`

---

## 🚀 Setup & Execution (Manual)

**1. Initialize Environment**
```bash
python -m venv venv
source venv/Scripts/activate  # On Windows
pip install -r requirements.txt
```

**2. Run Server**
```bash
uvicorn main:app --reload --port 8000
```

---

## 🐳 Docker Deployment

To deploy this microservice in a containerized environment without installing Python locally:

```bash
# Build the image
docker build -t sih26190-aiml .

# Run the container
docker run -d -p 8000:8000 sih26190-aiml
```

---

## 🧪 End-to-End Testing (Using Swagger UI)

To ensure the AI/ML service is working perfectly before integrating with the Java backend, you can test it directly:

1. **Start the Server:** Use the run scripts or Docker to start the API.
2. **Open Swagger UI:** Go to **[http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)** in your browser.
3. **Select Endpoint:** Click on `POST /api/v1/process-document`.
4. **Prepare the Test:** Click **"Try it out"**.
5. **Upload Image:** Click the "Choose File" button next to the `file` field. Select an image (like a fake FIR or a downloaded Aadhaar card sample).
6. **Execute:** Click the large blue **"Execute"** button.
7. **Verify Output:** Scroll down to the **"Server response"** section. You should see a `200` status code with the JSON body showing bounding boxes, document classification, and tamper scores.
