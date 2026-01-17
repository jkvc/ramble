@echo off

rem Gradle wrapper script for Windows

set APP_HOME=%~dp0

if defined JAVA_HOME (
    set JAVA_CMD=%JAVA_HOME%\bin\java
) else (
    set JAVA_CMD=java
)

set WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
    echo Gradle wrapper JAR not found. Please run from Android Studio first or download it.
    exit /b 1
)

"%JAVA_CMD%" -Dorg.gradle.appname=gradlew -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
