@echo off
REM Runs the full SAST suite (detekt + Android Lint).
REM detekt runs through the detektSast task, which forks its own JDK 21 process.

setlocal
cd /d "%~dp0.."

call gradlew.bat :app:detektSast :app:lintDebug %*

echo.
echo ==^> detekt: appuildeports\detektecho ==^> lint:   appuildeports\lint-results-debug.html
endlocal
