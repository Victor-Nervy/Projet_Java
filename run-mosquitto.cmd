@echo off
if exist "C:\Program Files\mosquitto\mosquitto.exe" (
    "C:\Program Files\mosquitto\mosquitto.exe" -v
) else (
    echo Mosquitto introuvable dans C:\Program Files\mosquitto\
    echo Alternative : lancer le broker Python local :
    echo   python scripts\local-mqtt-broker.py
    exit /b 1
)
