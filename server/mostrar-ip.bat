@echo off
REM Imprime las direcciones que tus companeros deben escribir en el campo "Server"
REM de la app, para conectarse al servidor Face-off de esta computadora.

echo.
echo   Direcciones para compartir (usa la de tu Wi-Fi):
echo.
powershell -NoProfile -Command "Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } | ForEach-Object { '      ' + $_.IPAddress + ':8080      (' + $_.InterfaceAlias + ')' }"
echo.
echo   El servidor se levanta con:  gradlew :server:run
echo.
pause
