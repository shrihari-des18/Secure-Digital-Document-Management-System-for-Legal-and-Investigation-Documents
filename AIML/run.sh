#!/bin/bash
echo "=========================================="
echo "Starting SIH26190 AI/ML Microservice..."
echo "=========================================="

if [ ! -d "venv" ]; then
    echo "[1/3] Creating Python Virtual Environment..."
    python3 -m venv venv
    
    echo "[2/3] Installing Dependencies..."
    source venv/bin/activate
    pip install -r requirements.txt
else
    echo "[1/2] Virtual Environment Found. Activating..."
    source venv/bin/activate
fi

echo "[SUCCESS] Running Uvicorn Server..."
python3 -m uvicorn main:app --reload --port 8000
