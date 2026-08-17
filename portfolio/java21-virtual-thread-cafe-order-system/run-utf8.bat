@echo off
chcp 65001 > nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8
set GRADLE_OPTS=-Dfile.encoding=UTF-8
echo === VIRTUAL THREAD VERSION UTF-8 RUN ===
call gradlew.bat run --console=plain
pause
