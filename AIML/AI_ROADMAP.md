# SIH26190: AI/ML Evolution Roadmap (Phase 2)

## Current State (Hackathon MVP)
Our current AI Microservice prioritizes **speed, privacy, and deterministic accuracy** using a hybrid pipeline:
1. **PaddleOCR** for deep-learning-based structural text extraction and pixel-accurate bounding box generation.
2. **Regex/Heuristics Engine** for instantaneous, 100% explainable Named Entity Recognition (NER) and Document Classification.
3. **Error Level Analysis (ELA)** for algorithmic image tamper detection.

This MVP avoids the "Cold Start" problem where high-quality, privacy-compliant datasets of sensitive Indian police documents (FIRs, Charge Sheets) are unavailable for training public models.

---

## Phase 2: Deep Learning Integration Strategy

As the platform scales and enters production, the AI subsystem will evolve from heuristic rules to a fine-tuned LLM/Transformer architecture. We will execute this transition in 5 deliberate steps:

### Step 1: Privacy-Preserving Data Bootstrapping (The "Snowball" Strategy)
- **Challenge:** Lack of unredacted, publicly available Indian legal documents.
- **Solution:** Deploy the current MVP. As investigating officers upload documents into the secure system over the first 6 months, we will use our **current Regex/PII Engine to completely redact and anonymize the data** before it enters a training bucket.
- **Result:** A massive, proprietary, and 100% privacy-compliant dataset of real legal documents safe for ML training.

### Step 2: Foundation Model Selection
- We will not use external APIs (like OpenAI GPT-4) to ensure no sensitive police data leaves the state's sovereign servers.
- We will select a lightweight, open-source Transformer architecture optimized for Indian dialects and legal contexts.
- **Candidates:** `IndicBERT` (AI4Bharat) or `Custom-LegalBERT`.

### Step 3: Fine-Tuning for Sequence Classification (Document Tagging)
- We will replace the keyword-based `classifier_service` with a Sequence Classification head trained on our bootstrapped dataset.
- The model will learn the semantic context of legal documents, allowing it to distinguish between nuanced forms (e.g., "Draft FIR" vs. "Final FIR") with >99% accuracy, even if standard keywords are misspelled or missing.

### Step 4: Fine-Tuning for Token Classification (Advanced NER)
- While our current regex perfectly catches structured PII (Aadhaar, Phone Numbers), it struggles with unstructured data.
- We will fine-tune the Transformer to perform advanced Token Classification, allowing it to dynamically identify and redact contextual entities such as:
  - `"Victim Name"`
  - `"Suspect Name"`
  - `"Weapon Used"`
  - `"Witness Addresses"`

### Step 5: High-Performance Edge Deployment
- To maintain the blazing-fast response times of our current FastAPI microservice, the fine-tuned PyTorch models will be exported to **ONNX** or compiled using **NVIDIA TensorRT**.
- This guarantees the heavy NLP models run efficiently on standardized CPU infrastructure or edge GPUs, minimizing infrastructure costs for the police departments.
