@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-25.0.2
set PATH=%JAVA_HOME%\bin;%PATH%
mvn javafx:run -Djavafx.mainClass=Launch
