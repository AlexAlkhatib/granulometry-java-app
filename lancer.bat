@echo off
rem Lance Granulometrie Studio depuis le paquet construit (target\).
setlocal
cd /d "%~dp0"

if defined JAVA_HOME (
  set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA=java"
)

if not exist "target\granulometrie-studio.jar" (
  echo Construction de l'application ^(mvn package^)...
  call mvn -q -DskipTests package
)

"%JAVA%" --module-path "target\lib\javafx" --add-modules javafx.controls,javafx.swing ^
  -cp "target\granulometrie-studio.jar;target\lib\h2-2.2.224.jar" ^
  fr.granulometrie.Launcher %*
endlocal
