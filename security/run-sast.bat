@echo off
REM Corre todo el SAST (detekt + Android Lint).
REM detekt corre con la tarea detektSast, que usa su propio proceso con JDK 21.

setlocal
cd /d "%~dp0.."

call gradlew.bat :app:detektSast :app:lintDebug %*

echo.
echo ==^> detekt: app\build\reports\detekt\
echo ==^> lint:   app\build\reports\lint-results-debug.html
endlocal
