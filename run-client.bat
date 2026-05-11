@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-26
set PATH=%JAVA_HOME%\bin;%PATH%
mvn javafx:run -Djavafx.mainClass=Launch
