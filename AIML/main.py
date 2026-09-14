from fastapi import FastAPI
from api.health import router as health_router
from api.process import router as process_router
from api.analyze import router as analyze_router

app = FastAPI(
    title="SIH26190 AI/ML Microservice",
    description="Microservice for OCR, NER, PII Redaction, Classification, Tamper Detection, and Dedup — SIH26190",
    version="2.0.0"
)

app.include_router(health_router, prefix="/api/v1")
app.include_router(process_router, prefix="/api/v1")
app.include_router(analyze_router, prefix="/ml/v1")   # Spring backend contract

@app.get("/")
async def root():
    return {"message": "SIH26190 AI/ML Microservice v2.0"}
