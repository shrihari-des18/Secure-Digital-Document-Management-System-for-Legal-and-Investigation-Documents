#!/bin/bash
echo "=========================================="
echo "Starting SIH26190 AI/ML Microservice..."
echo "=========================================="

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

if [ ! -d "venv" ]; then
    echo "[1/3] Creating Python Virtual Environment..."
    if command -v uv &> /dev/null; then
        uv venv venv --python 3.12
    elif [ -x "$HOME/.local/bin/uv" ]; then
        "$HOME/.local/bin/uv" venv venv --python 3.12
    elif command -v python3.12 &> /dev/null; then
        python3.12 -m venv venv
    else
        python3 -m venv venv
    fi

    echo "[2/3] Installing Dependencies..."
    source venv/bin/activate
    if [ -x "$HOME/.local/bin/uv" ]; then
        "$HOME/.local/bin/uv" pip install -r requirements.txt --python venv/bin/python
    else
        pip install -r requirements.txt
    fi
else
    echo "[1/2] Virtual Environment Found. Activating..."
    source venv/bin/activate
fi

echo "[SUCCESS] Running Uvicorn Server on port 8000..."
python3 -m uvicorn main:app --reload --port 8000

