@echo off
setlocal
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "PATH=%JAVA_HOME%\bin;%PATH%"

set CP=target/classes
set CP=%CP%;%USERPROFILE%/.m2/repository/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar
set CP=%CP%;%USERPROFILE%/.m2/repository/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar
set CP=%CP%;%USERPROFILE%/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar
set CP=%CP%;%USERPROFILE%/.m2/repository/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar

java -cp "%CP%" Network.AuctionServer 8989
pause