#!/bin/bash
echo "=========================================="
echo "Starting SIH26190 AI/ML Microservice..."
echo "=========================================="

# PaddlePaddle does not have wheels for Python 3.12 or 3.13 yet.
# We will intelligently search for a compatible Python version on the system.
PYTHON_CMD="python3"
for py in python3.10 python3.11 python3.9 python3.8; do
    if command -v $py &> /dev/null; then
        PYTHON_CMD=$py
        break
    fi
done

echo "Using Python executable: $PYTHON_CMD"

if [ ! -d "venv" ]; then
    echo "[1/3] Creating Python Virtual Environment..."
    $PYTHON_CMD -m venv venv
fi

echo "[2/3] Activating Virtual Environment..."
. venv/bin/activate

echo "[3/3] Syncing Dependencies..."
pip install -r requirements.txt

echo "[SUCCESS] Running Uvicorn Server..."
python -m uvicorn main:app --reload --port 8000
