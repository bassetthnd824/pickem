@echo off
setlocal
cd /d "%~dp0..\.."
call mvnw.cmd -pl apps/backend -Dexec.args="%*" compile exec:java
exit /b %ERRORLEVEL%
