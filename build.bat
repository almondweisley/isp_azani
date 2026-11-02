@echo off
rem ============================================================================
rem  build.bat  -  builds "Azani ISP.exe" with its own Java runtime inside.
rem
rem  Put this file in the isp_azani folder (the one holding db, model, dao,
rem  service, ui, lib, fonts and db.properties), then double-click it.
rem  Result:  dist\Azani ISP Setup.exe       the installer to hand in (needs Inno Setup 6)
rem           dist\Azani ISP\Azani ISP.exe   and   dist\Azani ISP.zip
rem
rem  db.properties goes inside the jar, so point it at the online database
rem  before you build, or the installed app only works on this PC.
rem
rem  Close the running app before you build again, or the old copy cannot be deleted.
rem ============================================================================
setlocal
cd /d "%~dp0"

set "APP=Azani ISP"
set "VERSION=1.0"
set "DRIVER=mysql-connector-j-26.7.0.jar"

rem ---- If the build cannot find Java, paste your JDK folder between = and " ----
rem ---- (IntelliJ: File > Project Structure > SDKs shows the path)            ----
set "JDK="

rem ---------------------------------------------------------------- find a JDK
if not defined JDK if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JDK=%JAVA_HOME%"
if not defined JDK for /d %%d in ("%USERPROFILE%\.jdks\*") do if exist "%%d\bin\jpackage.exe" set "JDK=%%d"
rem  (No brackets here on purpose: a path like "Program Files (x86)" would break an if ( ... ) block.)
set "BIN="
if defined JDK set "BIN=%JDK%\bin\"
if not defined JDK where jpackage >nul 2>&1 || goto :nojdk
if defined JDK echo Using JDK: %JDK%
if not defined JDK echo Using the JDK found on PATH

rem ---------------------------------------------------------------- checks
if not exist "lib\%DRIVER%" ( echo Missing lib\%DRIVER% & goto :fail )
if not exist "db.properties" ( echo Missing db.properties & goto :fail )
if not exist "fonts\PublicSans-Regular.ttf" echo Warning: fonts folder not found - the app will use Java's default font.
findstr /i /c:"localhost" /c:"127.0.0.1" db.properties >nul && echo Warning: db.properties points to this PC - the installed app will not reach the database on other PCs.

rem ---------------------------------------------------------------- find Inno Setup (optional)
set "ISCC="
for %%i in (ISCC.exe) do set "ISCC=%%~$PATH:i"
if not defined ISCC if exist "%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe" set "ISCC=%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe"
if not defined ISCC if exist "%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" set "ISCC=%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe"
if not defined ISCC echo Warning: Inno Setup 6 not found - no installer will be made. Install it with: winget install JRSoftware.InnoSetup

rem ---------------------------------------------------------------- 1 clean
if exist build rmdir /s /q build
if exist dist  rmdir /s /q dist
if exist dist ( echo Could not delete dist. Close the running app and try again. & goto :fail )
mkdir build\classes build\input

rem ---------------------------------------------------------------- 2 compile
echo [1/6] Compiling...
"%BIN%javac" -encoding UTF-8 -nowarn -d build\classes -cp "lib\%DRIVER%" db\*.java model\*.java dao\*.java service\*.java ui\*.java
if errorlevel 1 goto :fail

rem ---------------------------------------------------------------- 3 resources INSIDE the jar
rem  DBConnection and Theme look on the classpath first, so these are found
rem  however the exe is started.
echo [2/6] Adding db.properties and fonts...
copy /y db.properties build\classes\ >nul
if exist fonts xcopy fonts build\classes\fonts\ /e /i /q /y >nul

rem ---------------------------------------------------------------- 4 jar
echo [3/6] Making AzaniISP.jar...
> build\manifest.txt echo Class-Path: %DRIVER%
"%BIN%jar" --create --file build\input\AzaniISP.jar --manifest build\manifest.txt --main-class ui.MainFrame -C build\classes .
if errorlevel 1 goto :fail
copy /y "lib\%DRIVER%" build\input\ >nul

rem ---------------------------------------------------------------- 5 exe with bundled Java
echo [4/6] Packaging the .exe with its own Java (this takes a minute)...
"%BIN%jpackage" --type app-image --name "%APP%" --app-version %VERSION% --vendor "%APP%" --input build\input --main-jar AzaniISP.jar --main-class ui.MainFrame --dest dist
if errorlevel 1 goto :fail

rem ---------------------------------------------------------------- 6 zip for submission
echo [5/6] Zipping...
powershell -NoProfile -Command "Compress-Archive -Path 'dist\%APP%' -DestinationPath 'dist\%APP%.zip' -Force"
if errorlevel 1 goto :fail

rem ---------------------------------------------------------------- 7 installer
if not defined ISCC goto :done
echo [6/6] Making the installer...
"%ISCC%" /Q "/DAppVersion=%VERSION%" AzaniISP.iss
if errorlevel 1 goto :fail

:done
echo.
echo Done.
echo   Run:     dist\%APP%\%APP%.exe
echo   Zip:     dist\%APP%.zip
if defined ISCC echo   Install: dist\%APP% Setup.exe
pause
exit /b 0

:nojdk
echo Could not find a JDK with jpackage. Set JDK= near the top of build.bat.
:fail
echo.
echo BUILD FAILED - read the message above.
pause
exit /b 1
