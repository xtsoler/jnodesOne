@echo off
setlocal

rem === Resolve Java ===
if defined JAVA_HOME (
  if not exist "%JAVA_HOME%\bin\java.exe" (
    echo JAVA_HOME is set but invalid: %JAVA_HOME%
    set "JAVA_HOME="
  )
)

if not defined JAVA_HOME (
  for /f "delims=" %%J in ('where java 2^>nul') do (
    if not defined JAVA_HOME (
      for %%K in ("%%~dpJ..") do set "JAVA_HOME=%%~fK"
    )
  )
)

if not defined JAVA_HOME (
  for %%P in (
    "C:\Program Files\Java\jdk-21"
    "C:\Program Files\Java\jdk-17"
    "C:\Program Files\Eclipse Adoptium\jdk-21"
    "C:\Program Files\Eclipse Adoptium\jdk-17"
  ) do (
    if not defined JAVA_HOME if exist "%%~P\bin\java.exe" set "JAVA_HOME=%%~P"
  )
)

if not defined JAVA_HOME (
  echo JAVA_HOME not found. Set JAVA_HOME to your JDK install and rerun.
  exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JAVA_HOME is set but java.exe not found: %JAVA_HOME%
  exit /b 1
)

set "JRE_HOME=%JAVA_HOME%"
echo Using JAVA_HOME=%JAVA_HOME%

rem === Compile only ===
call mvn clean package
if errorlevel 1 (
  echo Maven compile failed.
  exit /b 1
)

echo Compile successful.
endlocal
