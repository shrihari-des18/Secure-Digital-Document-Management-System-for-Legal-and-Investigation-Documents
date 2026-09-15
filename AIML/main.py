from fastapi import FastAPI
from api.health import router as health_router
from api.process import router as process_router
from api.analyze import router as analyze_router

from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="SIH26190 AI/ML Microservice",
    description="Microservice for OCR, NER, PII Redaction, Classification, Tamper Detection, and Dedup — SIH26190",
    version="2.0.0"
)

# Add CORS middleware to allow the frontend to communicate with this API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, replace with the specific frontend URL
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_router, prefix="/api/v1")
app.include_router(process_router, prefix="/api/v1")
app.include_router(analyze_router, prefix="/ml/v1")   # Spring backend contract

@app.get("/")
async def root():
    return {"message": "SIH26190 AI/ML Microservice v2.0"}
