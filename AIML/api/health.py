from fastapi import APIRouter

router = APIRouter()

@router.get("/health", tags=["Health"])
async def health_check():
    """
    Health check endpoint to verify the microservice is running.
    """
    return {"status": "ok", "service": "AIML Microservice (SIH26190)"}
