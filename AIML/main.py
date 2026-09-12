from fastapi import FastAPI
from api.health import router as health_router

app = FastAPI(
    title="SIH26190 AI/ML Microservice",
    description="Microservice for OCR, NER, and PII Redaction for SIH26190",
    version="1.0.0"
)

# Include API routers
app.include_router(health_router, prefix="/api/v1")

@app.get("/")
async def root():
    return {"message": "Welcome to the SIH26190 AI/ML Microservice"}
