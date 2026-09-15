@echo off
echo ==========================================
echo Starting SIH26190 AI/ML Microservice...
echo ==========================================

IF NOT EXIST "venv" (
    echo [1/3] Creating Python Virtual Environment...
    python -m venv venv
    
    echo [2/3] Installing Dependencies...
    call venv\Scripts\activate
    pip install -r requirements.txt
) ELSE (
    echo [1/2] Virtual Environment Found. Activating...
    call venv\Scripts\activate
)

echo [SUCCESS] Running Uvicorn Server...
python -m uvicorn main:app --reload --port 8000
