@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-26
set PATH=%JAVA_HOME%\bin;%PATH%
java -cp "target/classes;%USERPROFILE%/.m2/repository/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar" Network.AuctionServer 8989
pause
