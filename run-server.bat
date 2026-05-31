@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

set M2_REPO=%USERPROFILE%\.m2\repository

set CLASSPATH=target\classes
set CLASSPATH=!CLASSPATH!;!M2_REPO!\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar
set CLASSPATH=!CLASSPATH!;!M2_REPO!\com\zaxxer\HikariCP\5.1.0\HikariCP-5.1.0.jar
set CLASSPATH=!CLASSPATH!;!M2_REPO!\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar
set CLASSPATH=!CLASSPATH!;!M2_REPO!\org\slf4j\slf4j-simple\1.7.36\slf4j-simple-1.7.36.jar

java -cp "!CLASSPATH!" Network.AuctionServer 8989
pause