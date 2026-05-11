@echo off
setlocal
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "PATH=%JAVA_HOME%\bin;%PATH%"
set BTL_LAUNCHED_BY_MAVEN=true
mvn javafx:run "-Djavafx.mainClass=Launch"
