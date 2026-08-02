@echo off
setlocal enabledelayedexpansion
title SLMS Builder

:: ─────────────────────────────────────────────────────────────────────────────
:: SLMS - No-Maven Build Script
:: Uses the system JDK (Java 24) - no Maven required
:: ─────────────────────────────────────────────────────────────────────────────

set "JDK_HOME=C:\Program Files\Java\jdk-24"
set "JAVAC=%JDK_HOME%\bin\javac.exe"
set "JAR_TOOL=%JDK_HOME%\bin\jar.exe"

set "ROOT=%~dp0"
set "BUNDLE_DIR=%ROOT%Smart Library Management System"
set "SRC_DIR=%ROOT%src\main\java"
set "RES_DIR=%ROOT%src\main\resources"
set "OLD_JAR=%BUNDLE_DIR%\app\SmartLibraryManagementSystem-1.0.0.jar"
set "BUILD_DIR=%ROOT%build_tmp"
set "CLASSES_DIR=%BUILD_DIR%\classes"
set "SOURCES_FILE=%BUILD_DIR%\sources.txt"

echo.
echo ============================================================
echo   SLMS No-Maven Builder
echo ============================================================
echo.

:: ── 1. Sanity checks ─────────────────────────────────────────────────────────
if not exist "%JAVAC%" (
    echo [ERROR] javac not found at: %JAVAC%
    pause & exit /b 1
)
if not exist "%JAR_TOOL%" (
    echo [ERROR] jar.exe not found at: %JAR_TOOL%
    pause & exit /b 1
)
if not exist "%OLD_JAR%" (
    echo [ERROR] Existing JAR not found at: %OLD_JAR%
    pause & exit /b 1
)

echo [OK] JDK          : %JDK_HOME%
echo [OK] Source root  : %SRC_DIR%
echo [OK] Resources    : %RES_DIR%
echo [OK] Target JAR   : %OLD_JAR%
echo.

:: ── 2. Clean temp build folder ───────────────────────────────────────────────
echo [1/5] Cleaning temp build folder...
if exist "%BUILD_DIR%" rd /s /q "%BUILD_DIR%"
mkdir "%CLASSES_DIR%"
echo       Done.

:: ── 3. Collect all .java source files into an @argfile ───────────────────────
::       javac @argfile expects one path per line, with forward slashes or
::       escaped backslashes. We use forward slashes to be safe.
echo [2/5] Collecting Java source files...
del /f /q "%SOURCES_FILE%" 2>nul
for /r "%SRC_DIR%" %%f in (*.java) do (
    set "FPATH=%%f"
    echo !FPATH:\=/! >> "%SOURCES_FILE%"
)
echo       Done.

:: ── 4. Compile ───────────────────────────────────────────────────────────────
echo [3/5] Compiling sources (this may take 15-30 seconds)...
"%JAVAC%" --release 17 -cp "%OLD_JAR%" -d "%CLASSES_DIR%" @"%SOURCES_FILE%"

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Compilation failed! See errors above.
    pause & exit /b 1
)
echo       Compilation successful.

:: ── 5. Copy fresh resources ──────────────────────────────────────────────────
echo [4/5] Copying resources (FXML, CSS, icons)...
xcopy /s /y /q "%RES_DIR%\*" "%CLASSES_DIR%\" >nul
echo       Resources copied.

:: ── 6. Backup + update the JAR in-place ─────────────────────────────────────
echo [5/5] Repacking JAR...
copy /y "%OLD_JAR%" "%OLD_JAR%.bak" >nul
echo       Backup saved: SmartLibraryManagementSystem-1.0.0.jar.bak

pushd "%CLASSES_DIR%"
"%JAR_TOOL%" uf "%OLD_JAR%" .
if %ERRORLEVEL% neq 0 (
    echo [ERROR] JAR update failed!
    popd & pause & exit /b 1
)
popd

:: ── 7. Cleanup ───────────────────────────────────────────────────────────────
rd /s /q "%BUILD_DIR%"

echo.
echo ============================================================
echo   BUILD SUCCESSFUL!
echo ============================================================
echo.
echo   Updated JAR  : %OLD_JAR%
echo   Backup saved : %OLD_JAR%.bak
echo.
echo   Run the app:  double-click run.bat  or
echo                 Smart Library Management System.exe
echo.
pause
