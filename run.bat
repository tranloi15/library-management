@echo off
title Library Management System
echo ========================================================
echo   Starting Library Management System (Spring Boot)
echo ========================================================

REM Tu dong phat hien JDK tuong thich (uu tien openjdk-22)
if exist "C:\Users\hn\.jdks\openjdk-22.0.2" (
    set "JAVA_HOME=C:\Users\hn\.jdks\openjdk-22.0.2"
) else if exist "C:\Users\hn\.jdks\jdk-25.0.2" (
    set "JAVA_HOME=C:\Users\hn\.jdks\jdk-25.0.2"
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo Using JAVA_HOME: %JAVA_HOME%
)

if exist ".\mvnw.cmd" (
    call .\mvnw.cmd spring-boot:run
) else if exist "C:\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd" (
    "C:\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run
) else (
    mvn spring-boot:run
)

pause
