@echo off
setlocal
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "PATH=%JAVA_HOME%\bin;%PATH%"
set "REPO=%USERPROFILE%\.m2\repository"
java -cp "target/classes;%REPO%\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar;%REPO%\com\zaxxer\HikariCP\5.1.0\HikariCP-5.1.0.jar;%REPO%\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar;%REPO%\org\slf4j\slf4j-simple\1.7.36\slf4j-simple-1.7.36.jar" Network.AuctionServer 8989
pause１