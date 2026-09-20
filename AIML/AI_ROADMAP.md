# SIH26190 — AI/ML Microservice: What's Built, What's Next

## Current state (as of Sept 2026)

The microservice is a working FastAPI app running on PaddleOCR v3.7 + Python 3.13.
All five services below are implemented and wired to the API.

| Service | File | Status |
|---|---|---|
| OCR (text + bounding boxes) | `services/ocr_service.py` | ✅ Working |
| NER / PII regex engine | `services/ner_service.py` | ✅ Working |
| Document classifier | `services/classifier_service.py` | ✅ Working |
| Tamper detection (ELA) | `services/tamper_service.py` | ✅ Working |
| PDF → image conversion | `services/pdf_service.py` | ✅ Working |
| Perceptual hash dedup | `services/dedup_service.py` | ✅ Working |

Two API endpoints:

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/process-document` | Swagger/manual testing — accepts a file upload |
| `POST /ml/v1/analyze` | Spring backend contract — accepts signed URL + task list |

---

## What the NER engine currently catches

- `AADHAAR` — 12-digit format (with or without spaces/dashes)
- `PHONE_NUMBER` — Indian mobile formats (+91, 10-digit starting 6-9)
- `BNS_SECTION` — Bharatiya Nyaya Sanhita section references
- `IPC_SECTION` — Indian Penal Code section references

---

## Near-term improvements (no ML training required)

These are additive — regex or lightweight library additions to `ner_service.py`.

### 1. Expand NER entity coverage
Add patterns for:
- `PAN_CARD` — `[A-Z]{5}[0-9]{4}[A-Z]`
- `PASSPORT` — Indian passport format (`[A-Z][0-9]{7}`)
- `VEHICLE_REG` — state code + district code + number (e.g. `MH12AB1234`)
- `DATE` — DD/MM/YYYY and written-out dates (useful for custody timeline extraction)
- `CASE_NUMBER` — `FIR No.`, `Crime No.`, RC/CC patterns

No new dependencies. ETA: 1–2 hours.

### 2. Name extraction (spaCy)
Add `spacy` (`en_core_web_sm`) for `PERSON` and `ORG` entities — catches names like
"Victim: Ramesh Kumar" that regex can't handle structurally.
New dependency: `spacy` (~30 MB model download).
Update `ner_service.py` to run spaCy on each text block after the regex pass.
ETA: 2–3 hours.

### 3. ELA threshold calibration
The current `ela_threshold = 5.5` is a rough heuristic.
- Collect ~20 known-clean and ~20 known-tampered Indian document images.
- Plot the score distribution and pick a threshold that minimises false positives.
- Add a `tamper_confidence` field alongside `is_tampered`.
ETA: half a day with test images.

### 4. EXIF metadata consistency check
Augment `tamper_service.py` with a second signal:
- Use `Pillow` to extract EXIF data.
- Flag if `Software`, `DateTimeOriginal`, or GPS fields look inconsistent with the claimed device/document type.
- Return as a separate `metadata_flags` list alongside the ELA score.
ETA: 2–3 hours.

### 5. Async job pattern
Heavy OCR on a 20-page PDF blocks the request thread.
- Add a simple in-process job store (dict + asyncio lock) or use FastAPI `BackgroundTasks`.
- `/ml/v1/analyze` returns `202 Accepted` + `job_id` immediately.
- Add `GET /ml/v1/jobs/{job_id}` for the backend to poll.
- The `callback_url` field in the request is already in the schema — implement the POST-back here.
ETA: half a day.

---

## Medium-term (post-hackathon, needs data)

### Data bootstrapping (the "snowball")
The current regex/PII engine redacts Aadhaar, phone, and legal sections from every document
processed through the system. That redacted data is safe to accumulate as a training corpus.
Store it in a separate, access-controlled bucket from day one — even if training doesn't happen
for months, the data collection starts automatically.

### Replace keyword classifier with a transformer
Once ~500 labelled document examples are collected:
- Fine-tune `IndicBERT` (AI4Bharat) or `distilbert-base-uncased` for sequence classification.
- Target labels: FIR, CHARGE_SHEET, SEIZURE_MEMO, FORENSIC_REPORT, COURT_ORDER, UNKNOWN.
- Replace `classifier_service.py` — the API contract stays identical.
- Expected accuracy gain: catches documents where keywords are misspelled or missing.

### Replace regex NER with token classification
Fine-tune the same transformer with a token classification head on the accumulated corpus.
Adds contextual entities (victim name, suspect name, witness address) that regex cannot catch.
API contract unchanged — just more entity types returned.

### ONNX export for production
Once fine-tuned models exist:
- Export PyTorch → ONNX.
- Run via `onnxruntime` in the FastAPI service.
- Removes the PyTorch runtime from the production container (~600 MB saved).
- Inference latency drops 2–3× on CPU.

---

## What this roadmap is NOT doing

The following are out of scope until there is real labelled data and production infrastructure:

- Training from scratch on raw LLMs
- NVIDIA TensorRT (requires GPU infra the police departments don't have)
- Semantic search embeddings (deferred until OpenSearch is deployed by the backend team)
- Any external API calls for ML (OpenAI, Gemini, etc.) — no sensitive data leaves state servers
