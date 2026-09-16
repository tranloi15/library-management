@echo off
title Library Management System
echo ========================================================
echo   Starting Library Management System (Spring Boot)
echo ========================================================
set "JAVA_HOME=C:\Users\hn\.antigravity-ide\extensions\redhat.java-1.56.0-win32-x64\jre\21.0.12.1-win32-x86_64"
set "PATH=%JAVA_HOME%\bin;%PATH%"
"C:\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run
pause
