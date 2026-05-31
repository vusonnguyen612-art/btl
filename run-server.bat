@echo off
setlocal
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "PATH=%JAVA_HOME%\bin;%PATH%"
java -cp "target/classes;%USERPROFILE%/.m2/repository/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar;%USERPROFILE%/.m2/repository/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar" Network.AuctionServer 8989
pause