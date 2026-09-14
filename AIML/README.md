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
  - `AADHAAR`: 12-digit format
  - `PHONE_NUMBER`: Indian formats
  - `BNS_SECTION`: Bharatiya Nyaya Sanhita sections
  - `IPC_SECTION`: Indian Penal Code sections
- **Correlation:** Associates the regex-matched text with the bounding box inherited from the OCR pipeline, formatting it as `[min_x, min_y, max_x, max_y]`.

---

## 📡 API Contract (FastAPI)

### `POST /api/v1/process-document`
Accepts a `multipart/form-data` file upload and returns the extracted entities and text.

**Response Schema (`DocumentProcessResponse`):**
```json
{
  "document_id": "uuid-string",
  "status": "SUCCESS",
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

### `GET /api/v1/health`
Standard health check returning `{"status": "ok", "service": "AIML Microservice (SIH26190)"}`.

---

## 🚀 Setup & Execution

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

**3. Test Environment**
Navigate to `http://localhost:8000/docs` to use the built-in Swagger UI for testing file uploads.

---

## 🧪 End-to-End Testing (Using Swagger UI)

To ensure the AI/ML service is working perfectly before integrating with the Java backend, you can test it directly:

1. **Start the Server:** Ensure your virtual environment is active and run `uvicorn main:app --reload --port 8000`.
2. **Open Swagger UI:** Go to **[http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)** in your browser.
3. **Select Endpoint:** Click on `POST /api/v1/process-document`.
4. **Prepare the Test:** Click **"Try it out"**.
5. **Upload Image:** Click the "Choose File" button next to the `file` field. Select an image (like a fake FIR or a downloaded Aadhaar card sample).
6. **Execute:** Click the large blue **"Execute"** button.
7. **Verify Output:** Scroll down to the **"Server response"** section. You should see a `200` status code with a JSON body similar to this:
```json
{
  "document_id": "uuid-string",
  "status": "SUCCESS",
  "extracted_text": "...",
  "entities": [
    {
      "entity_type": "AADHAAR",
      "value": "9876 5432 1098",
      "confidence": 1.0,
      "bounding_box": {
        "min_x": 219.0,
        "min_y": 319.0,
        "max_x": 510.0,
        "max_y": 346.0
      }
    }
  ]
}
```
If you see the `bounding_box` coordinates attached to your PII, the pipeline is fully operational!
