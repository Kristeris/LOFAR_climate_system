@echo off
cd /d "%~dp0"
REM Windows batch script to run SensorOutput.py

REM Execute the Python script
python SensorOutput.py

REM Pause to see the output (optional - comment out if running automatically)
REM pause