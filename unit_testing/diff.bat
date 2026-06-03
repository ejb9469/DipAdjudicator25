@echo off
setlocal EnableDelayedExpansion



REM  ## CONSTANTS ##
SET N_RUNS=10
SET WAIT_ms=250
SET WAIT_LONG_ms=5000
SET TMPFOLDER_NAME=difftmp
SET OUTFOLDER_PREFIX=diff-run


REM  ## create `.\difftmp` (if necessary)
cd /d  "%~dp0"
SET "cwd=%cd%"

if not exist  %TMPFOLDER_NAME% (
	mkdir  %TMPFOLDER_NAME%
)
cd  %TMPFOLDER_NAME%


REM  ## wipe dir of `.log` files (just in case)
del /f /q  .\*.log  2>nul


echo. & echo.
echo Executing DipAdj25::TestCaseManager.main() -- %N_RUNS% times:

REM  ## copy baseline `latest.log` to tmpdir
copy /Y  ^
	"C:\Users\tort\Desktop\DipAdjudicator25\latest.log"  ^
      .\init_latest.log  2>&1 >nul

REM  ## run the program 20 times (make sure the environment is correct!)
cd /d  "C:\Users\tort\Desktop\DipAdjudicator25"
echo.
for /L %%i in (1,1,!N_RUNS!) do (
	echo   Run #%%i...
	"C:\Program Files\Java\jdk-25\bin\java.exe"  ^
			"-javaagent:C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.3\lib\idea_rt.jar=61306"  ^
			-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8  ^
			-classpath "C:\Users\tort\Desktop\DipAdjudicator25\out\production\DipAdjudicator25" TestCaseManager  ^
		2>&1  > "%cwd%\%TMPFOLDER_NAME%\run%%i.log"
	echo     ...done^^!
	powershell -nop -c  ^
		"sleep -m  %WAIT_ms%"
)
cd /d  "%cwd%\%TMPFOLDER_NAME%"


echo.
echo   Diffing...
REM  ## Compare all runs to `init_latest.log` --> diffs
for /L %%i in (1,1,!N_RUNS!) do (
	fc /N /W  init_latest.log run%%i.log  > .\diff%%i.log
)


REM  ## find smallest diff(s) [e.g. only timestamps differ]
SET "line_count=999999"
for /F %%N in ('type diff1.log ^| find /v /c ""') do (
	SET "line_count=%%N"
)
for /L %%i in (2,1,!N_RUNS!) do (
	for /F %%N in ('type diff%%i.log ^| find /v /c ""') do (
		if %%N LSS !line_count! (
			SET "line_count=%%N"
		)
	)
)


REM  ## remove smallest diff(s)
for /L %%i in (1,1,!N_RUNS!) do (
	for /F %%N in ('type diff%%i.log ^| find /v /c ""') do (
		if %%N EQU !line_count! (
			del /f /q  .\diff%%i.log
		)
	)
)

echo     ...done^^!
powershell -nop -c  ^
	"sleep -m  %WAIT_ms%"


REM  ## rename tmpdir to prefix+timestamp
cd ..
SET foldername=%OUTFOLDER_PREFIX%_%date:~10,4%%date:~4,2%%date:~7,2%-%time:~0,2%%time:~3,2%%time:~6,2%
rename "%TMPFOLDER_NAME%" "%foldername%"

echo.
echo Logfiles path:
echo     "%cwd%\%foldername%\*.log"
echo.


REM  ## hang for a few secs (rather than resorting to `pause`)
powershell -nop -c  ^
	"sleep -m %WAIT_LONG_ms%"