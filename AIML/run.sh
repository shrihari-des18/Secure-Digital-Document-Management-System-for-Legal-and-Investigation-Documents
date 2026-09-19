#!/bin/bash
echo "=========================================="
echo "Starting SIH26190 AI/ML Microservice..."
echo "=========================================="

if [ ! -d "venv" ]; then
    echo "[1/3] Creating Python Virtual Environment..."
    python3 -m venv venv
fi

echo "[2/3] Activating Virtual Environment..."
. venv/bin/activate

echo "[3/3] Syncing Dependencies..."
pip install -r requirements.txt

echo "[SUCCESS] Running Uvicorn Server..."
python3 -m uvicorn main:app --reload --port 8000
