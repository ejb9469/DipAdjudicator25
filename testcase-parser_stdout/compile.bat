@echo off

pushd "%~dp0"
cd ..
rd /s /q out
javac -d "out/" src/*.java
xcopy /y /e /i "src/testgames" "out/testgames/"
xcopy /y /e /i "src/testgames_solutions" "out/testgames_solutions/"