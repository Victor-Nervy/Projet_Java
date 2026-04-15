@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -jar backend-server\target\backend-server-1.0.0-SNAPSHOT.jar %*
