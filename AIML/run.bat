@echo off
echo ==========================================
echo Starting SIH26190 AI/ML Microservice...
echo ==========================================

IF NOT EXIST "venv" (
    echo [1/3] Creating Python Virtual Environment...
    python -m venv venv
)

echo [2/3] Activating Virtual Environment...
call venv\Scripts\activate

echo [3/3] Syncing Dependencies...
pip install -r requirements.txt

echo [SUCCESS] Running Uvicorn Server...
python -m uvicorn main:app --reload --port 8000
