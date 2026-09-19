#!/bin/bash
echo "=========================================="
echo "Starting SIH26190 AI/ML Microservice..."
echo "=========================================="

# Smart Python Version Selector
PYTHON_CMD="python3"
for py in python3.10 python3.11 python3.9 python3.8; do
    if command -v $py &> /dev/null; then
        PYTHON_CMD=$py
        break
    fi
done
echo "Using Python executable: $PYTHON_CMD"

# Create Virtual Environment safely
if [ ! -d "venv" ]; then
    echo "[1/3] Creating Python Virtual Environment..."
    $PYTHON_CMD -m venv venv
    
    # CRITICAL LINUX FIX: Check if venv creation actually worked!
    if [ ! -f "venv/bin/activate" ]; then
        echo ""
        echo "❌ FATAL ERROR: Virtual Environment creation failed!"
        echo "This usually means you are on Ubuntu/Debian and are missing the 'venv' package."
        echo "Please run this command in your terminal, then try again:"
        echo ""
        echo "    sudo apt-get install -y python3-venv"
        echo ""
        exit 1
    fi
fi

echo "[2/3] Activating Virtual Environment..."
. venv/bin/activate

echo "[3/3] Syncing Dependencies..."
pip install --upgrade pip
pip install -r requirements.txt

echo "[SUCCESS] Running Uvicorn Server..."
python -m uvicorn main:app --reload --port 8000
