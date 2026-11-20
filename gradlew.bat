@echo off
setlocal
set "APP_HOME=%~dp0"
if not defined JAVA_HOME (
  if exist "C:\Program Files\JetBrains\PyCharm 2025.3.3\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\JetBrains\PyCharm 2025.3.3\jbr"
  )
)
if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java.exe"
)
"%JAVA_EXE%" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
