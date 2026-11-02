@echo off
rem ============================================================================
rem  build.bat  -  builds "Azani ISP Setup.exe": the app with its own Java and
rem  its own MySQL server inside, so it runs on any Windows PC without setup.
rem
rem  Put this file in the isp_azani folder (the one holding db, model, dao,
rem  service, ui, lib, fonts and the SQL scripts), then double-click it.
rem  Result:  dist\Azani ISP Setup.exe       the installer to hand in (needs Inno Setup 6)
rem           dist\Azani ISP\Azani ISP.exe   and   dist\Azani ISP.zip
rem
rem  The bundled server is copied from the MySQL Server installed on this PC,
rem  and its sample data comes from the DDL and DML scripts named below.
rem
rem  Close the running app before you build again, or the old copy cannot be deleted.
rem ============================================================================
setlocal
cd /d "%~dp0"

set "APP=Azani ISP"
set "VERSION=1.0"
set "DRIVER=mysql-connector-j-26.7.0.jar"
set "DDL=azani_db_ddl.sql"
set "DML=latest_dml_azani.sql"

rem ---- If the build cannot find Java or MySQL, paste its folder between = and " ----
rem ---- (IntelliJ: File > Project Structure > SDKs shows the JDK path)            ----
set "JDK="
set "MYSQL_HOME="

rem ---------------------------------------------------------------- find a JDK
if not defined JDK if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JDK=%JAVA_HOME%"
if not defined JDK for /d %%d in ("%USERPROFILE%\.jdks\*") do if exist "%%d\bin\jpackage.exe" set "JDK=%%d"
rem  (No brackets here on purpose: a path like "Program Files (x86)" would break an if ( ... ) block.)
set "BIN="
if defined JDK set "BIN=%JDK%\bin\"
if not defined JDK where jpackage >nul 2>&1 || goto :nojdk
if defined JDK echo Using JDK: %JDK%
if not defined JDK echo Using the JDK found on PATH

rem ---------------------------------------------------------------- find MySQL Server
if not defined MYSQL_HOME for /d %%d in ("%ProgramFiles%\MySQL\MySQL Server *") do if exist "%%d\bin\mysqld.exe" set "MYSQL_HOME=%%d"
if not defined MYSQL_HOME goto :nomysql
echo Using MySQL: %MYSQL_HOME%

rem ---------------------------------------------------------------- checks
if not exist "lib\%DRIVER%" ( echo Missing lib\%DRIVER% & goto :fail )
if not exist "%DDL%" ( echo Missing %DDL% & goto :fail )
if not exist "%DML%" ( echo Missing %DML% & goto :fail )
if not exist "fonts\PublicSans-Regular.ttf" echo Warning: fonts folder not found - the app will use Java's default font.

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
echo [1/7] Compiling...
"%BIN%javac" -encoding UTF-8 -nowarn -d build\classes -cp "lib\%DRIVER%" db\*.java model\*.java dao\*.java service\*.java ui\*.java
if errorlevel 1 goto :fail

rem ---------------------------------------------------------------- 3 fonts INSIDE the jar
rem  Theme looks on the classpath first, so the fonts are found however the exe is started.
rem  db.properties stays out: the installed app uses its own server (db\LocalServer.java).
echo [2/7] Adding fonts...
if exist fonts xcopy fonts build\classes\fonts\ /e /i /q /y >nul

rem ---------------------------------------------------------------- 4 jar
echo [3/7] Making AzaniISP.jar...
> build\manifest.txt echo Class-Path: %DRIVER%
"%BIN%jar" --create --file build\input\AzaniISP.jar --manifest build\manifest.txt --main-class ui.MainFrame -C build\classes .
if errorlevel 1 goto :fail
copy /y "lib\%DRIVER%" build\input\ >nul

rem ---------------------------------------------------------------- 5 MySQL server with the sample data
rem  Only what mysqld.exe needs: the DLLs it loads (this list is for MySQL 8.4), the
rem  Visual C++ runtime (a fresh Windows may lack it), share (messages, character sets)
rem  and lib\private (the data REGEXP needs).
rem  The exe is renamed so the uninstaller can stop it without touching any other MySQL.
echo [4/7] Preparing the app's own MySQL server and its sample data...
set "SRV=build\input\mysql"
mkdir "%SRV%\bin"
copy /y "%MYSQL_HOME%\bin\mysqld.exe" "%SRV%\bin\azani-mysqld.exe" >nul || goto :fail
for %%f in (abseil_dll.dll libcrypto-3-x64.dll libssl-3-x64.dll libprotobuf-lite.dll) do copy /y "%MYSQL_HOME%\bin\%%f" "%SRV%\bin\" >nul || goto :fail
for %%f in (msvcp140.dll vcruntime140.dll vcruntime140_1.dll) do copy /y "%SystemRoot%\System32\%%f" "%SRV%\bin\" >nul || goto :fail
xcopy "%MYSQL_HOME%\share" "%SRV%\share\" /e /i /q /y >nul || goto :fail
xcopy "%MYSQL_HOME%\lib\private" "%SRV%\lib\private\" /e /i /q /y >nul || goto :fail
copy /y "%MYSQL_HOME%\LICENSE" "%SRV%\" >nul
"%BIN%java" -cp "build\classes;lib\%DRIVER%" db.LocalServer "%SRV%" build\mysql-error.log "%DDL%" "%DML%"
if errorlevel 1 goto :fail

rem ---------------------------------------------------------------- 6 exe with bundled Java
rem  $APPDIR is the app folder of the installed program; the launcher fills it in at run time.
echo [5/7] Packaging the .exe with its own Java (this takes a minute)...
"%BIN%jpackage" --type app-image --name "%APP%" --app-version %VERSION% --vendor "%APP%" --input build\input --main-jar AzaniISP.jar --main-class ui.MainFrame --java-options "-Dazani.mysql=$APPDIR\mysql" --dest dist
if errorlevel 1 goto :fail

rem ---------------------------------------------------------------- 7 zip
echo [6/7] Zipping...
powershell -NoProfile -Command "Compress-Archive -Path 'dist\%APP%' -DestinationPath 'dist\%APP%.zip' -Force"
if errorlevel 1 goto :fail

rem ---------------------------------------------------------------- 8 installer
if not defined ISCC goto :done
echo [7/7] Making the installer...
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
goto :fail
:nomysql
echo Could not find MySQL Server. Set MYSQL_HOME= near the top of build.bat.
:fail
echo.
echo BUILD FAILED - read the message above.
pause
exit /b 1
