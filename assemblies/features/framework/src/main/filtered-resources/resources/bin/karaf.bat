@echo off
rem
rem
rem    Licensed to the Apache Software Foundation (ASF) under one or more
rem    contributor license agreements.  See the NOTICE file distributed with
rem    this work for additional information regarding copyright ownership.
rem    The ASF licenses this file to You under the Apache License, Version 2.0
rem    (the "License"); you may not use this file except in compliance with
rem    the License.  You may obtain a copy of the License at
rem
rem       http://www.apache.org/licenses/LICENSE-2.0
rem
rem    Unless required by applicable law or agreed to in writing, software
rem    distributed under the License is distributed on an "AS IS" BASIS,
rem    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem    See the License for the specific language governing permissions and
rem    limitations under the License.
rem

if not "%ECHO%" == "" echo %ECHO%

setlocal
set DIRNAME=%~dp0%
set PROGNAME=%~nx0%
set ARGS=%*

rem Sourcing environment settings for karaf similar to tomcats setenv
SET KARAF_SCRIPT="karaf.bat"
if exist "%DIRNAME%setenv.bat" (
  call "%DIRNAME%setenv.bat"
)

rem Check console window title. Set to Karaf by default
if not "%KARAF_TITLE%" == "" (
    title %KARAF_TITLE%
) else (
    title Karaf
)

rem Check/Set up some easily accessible MIN/MAX params for JVM mem usage
if "%JAVA_MIN_MEM%" == "" (
    set JAVA_MIN_MEM=128M
)
if "%JAVA_MAX_MEM%" == "" (
    set JAVA_MAX_MEM=512M
)

goto BEGIN

:warn
    echo %PROGNAME%: %*
goto :EOF

:BEGIN

rem # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

if not "%KARAF_HOME%" == "" (
    call :warn Ignoring predefined value for KARAF_HOME
)
set KARAF_HOME=%DIRNAME%..
if not exist "%KARAF_HOME%" (
    call :warn KARAF_HOME is not valid: "%KARAF_HOME%"
    goto END
)

if not "%KARAF_BASE%" == "" (
    if not exist "%KARAF_BASE%" (
       call :warn KARAF_BASE is not valid: "%KARAF_BASE%"
       goto END
    )
)
if "%KARAF_BASE%" == "" (
  set "KARAF_BASE=%KARAF_HOME%"
)

if not "%KARAF_DATA%" == "" (
    if not exist "%KARAF_DATA%" (
        call :warn KARAF_DATA is not valid: "%KARAF_DATA%"
        goto END
    )
)
if "%KARAF_DATA%" == "" (
    set "KARAF_DATA=%KARAF_BASE%\data"
)        

set LOCAL_CLASSPATH=%CLASSPATH%
set JAVA_MODE=-server
if not exist "%JAVA_HOME%\bin\server\jvm.dll" (
    if not exist "%JAVA_HOME%\jre\bin\server\jvm.dll" (
        echo WARNING: Running karaf on a Java HotSpot Client VM because server-mode is not available.
        echo Install Java Developer Kit to fix this.
        echo For more details see http://java.sun.com/products/hotspot/whitepaper.html#client
        set JAVA_MODE=-client
    )
)
set DEFAULT_JAVA_OPTS=%JAVA_MODE% -Xms%JAVA_MIN_MEM% -Xmx%JAVA_MAX_MEM% -Dderby.system.home="%KARAF_DATA%\derby" -Dderby.storage.fileSyncTransactionLog=true -Dcom.sun.management.jmxremote

rem Check some easily accessible MIN/MAX params for JVM mem usage
if not "%JAVA_PERM_MEM%" == "" (
    set DEFAULT_JAVA_OPTS=%DEFAULT_JAVA_OPTS% -XX:PermSize=%JAVA_PERM_MEM%
)
if not "%JAVA_MAX_PERM_MEM%" == "" (
    set DEFAULT_JAVA_OPTS=%DEFAULT_JAVA_OPTS% -XX:MaxPermSize=%JAVA_MAX_PERM_MEM%
)

set CLASSPATH=%LOCAL_CLASSPATH%;%KARAF_BASE%\conf
set DEFAULT_JAVA_DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

if "%LOCAL_CLASSPATH%" == "" goto :KARAF_CLASSPATH_EMPTY
    set CLASSPATH=%LOCAL_CLASSPATH%;%KARAF_BASE%\conf
    goto :KARAF_CLASSPATH_END
:KARAF_CLASSPATH_EMPTY
    set CLASSPATH=%KARAF_BASE%\conf
:KARAF_CLASSPATH_END

rem Setup Karaf Home
if exist "%KARAF_HOME%\conf\karaf-rc.cmd" call %KARAF_HOME%\conf\karaf-rc.cmd
if exist "%HOME%\karaf-rc.cmd" call %HOME%\karaf-rc.cmd

rem Support for loading native libraries
set PATH=%PATH%;%KARAF_BASE%\lib;%KARAF_HOME%\lib

rem Setup the Java Virtual Machine
if not "%JAVA%" == "" goto :Check_JAVA_END
    if not "%JAVA_HOME%" == "" goto :TryJDKEnd
        call :warn JAVA_HOME not set; results may vary
:TryJRE
    start /w regedit /e __reg1.txt "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment"
    if not exist __reg1.txt goto :TryJDK
    type __reg1.txt | find "CurrentVersion" > __reg2.txt
    if errorlevel 1 goto :TryJDK
    for /f "tokens=2 delims==" %%x in (__reg2.txt) do set JavaTemp=%%~x
    if errorlevel 1 goto :TryJDK
    set JavaTemp=%JavaTemp%##
    set JavaTemp=%JavaTemp:                ##=##%
    set JavaTemp=%JavaTemp:        ##=##%
    set JavaTemp=%JavaTemp:    ##=##%
    set JavaTemp=%JavaTemp:  ##=##%
    set JavaTemp=%JavaTemp: ##=##%
    set JavaTemp=%JavaTemp:##=%
    del __reg1.txt
    del __reg2.txt
    start /w regedit /e __reg1.txt "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment\%JavaTemp%"
    if not exist __reg1.txt goto :TryJDK
    type __reg1.txt | find "JavaHome" > __reg2.txt
    if errorlevel 1 goto :TryJDK
    for /f "tokens=2 delims==" %%x in (__reg2.txt) do set JAVA_HOME=%%~x
    if errorlevel 1 goto :TryJDK
    del __reg1.txt
    del __reg2.txt
    goto TryJDKEnd
:TryJDK
    start /w regedit /e __reg1.txt "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit"
    if not exist __reg1.txt (
        goto TryRegJRE
    )
    type __reg1.txt | find "CurrentVersion" > __reg2.txt
    if errorlevel 1 (
        goto TryRegJRE
    )
    for /f "tokens=2 delims==" %%x in (__reg2.txt) do set JavaTemp=%%~x
    if errorlevel 1 (
        goto TryRegJRE
    )
    set JavaTemp=%JavaTemp%##
    set JavaTemp=%JavaTemp:                ##=##%
    set JavaTemp=%JavaTemp:        ##=##%
    set JavaTemp=%JavaTemp:    ##=##%
    set JavaTemp=%JavaTemp:  ##=##%
    set JavaTemp=%JavaTemp: ##=##%
    set JavaTemp=%JavaTemp:##=%
    del __reg1.txt
    del __reg2.txt
    start /w regedit /e __reg1.txt "HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit\%JavaTemp%"
    if not exist __reg1.txt (
        goto TryRegJRE
    )
    type __reg1.txt | find "JavaHome" > __reg2.txt
    if errorlevel 1 (
        goto TryRegJRE
    )
    for /f "tokens=2 delims==" %%x in (__reg2.txt) do set JAVA_HOME=%%~x
    if errorlevel 1 (
        goto TryRegJRE
    )
    del __reg1.txt
    del __reg2.txt
:TryRegJRE
    rem try getting the JAVA_HOME from registry
    FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY "HKLM\Software\JavaSoft\Java Runtime Environment" /v CurrentVersion`) DO (
       set JAVA_VERSION=%%A
    )
    FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY "HKLM\Software\JavaSoft\Java Runtime Environment\%JAVA_VERSION%" /v JavaHome`) DO (
       set JAVA_HOME=%%A %%B
    )
    if not exist "%JAVA_HOME%" (
       goto TryRegJDK
	)
	goto TryJDKEnd
:TryRegJDK
    rem try getting the JAVA_HOME from registry
    FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY "HKLM\Software\JavaSoft\Java Development Kit" /v CurrentVersion`) DO (
       set JAVA_VERSION=%%A
    )
    FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY "HKLM\Software\JavaSoft\Java Development Kit\%JAVA_VERSION%" /v JavaHome`) DO (
       set JAVA_HOME=%%A %%B
    )
    if not exist "%JAVA_HOME%" (
       call :warn Unable to retrieve JAVA_HOME from Registry
    )
	goto TryJDKEnd
:TryJDKEnd
    if not exist "%JAVA_HOME%" (
        call :warn JAVA_HOME is not valid: "%JAVA_HOME%"
        goto END
    )
    set JAVA=%JAVA_HOME%\bin\java
:Check_JAVA_END

if "%JAVA_OPTS%" == "" set JAVA_OPTS=%DEFAULT_JAVA_OPTS%

if "%KARAF_DEBUG%" == "" goto :KARAF_DEBUG_END
    if "%1" == "stop" goto :KARAF_DEBUG_END
    if "%1" == "client" goto :KARAF_DEBUG_END
    rem Use the defaults if JAVA_DEBUG_OPTS was not set
    if "%JAVA_DEBUG_OPTS%" == "" set JAVA_DEBUG_OPTS=%DEFAULT_JAVA_DEBUG_OPTS%

    set "JAVA_OPTS=%JAVA_DEBUG_OPTS% %JAVA_OPTS%"
    call :warn Enabling Java debug options: %JAVA_DEBUG_OPTS%
:KARAF_DEBUG_END

if "%KARAF_PROFILER%" == "" goto :KARAF_PROFILER_END
    set KARAF_PROFILER_SCRIPT=%KARAF_HOME%\conf\profiler\%KARAF_PROFILER%.cmd

    if exist "%KARAF_PROFILER_SCRIPT%" goto :KARAF_PROFILER_END
    call :warn Missing configuration for profiler '%KARAF_PROFILER%': %KARAF_PROFILER_SCRIPT%
    goto END
:KARAF_PROFILER_END

rem Setup the classpath
pushd "%KARAF_HOME%\lib"
for %%G in (karaf*.jar) do call:APPEND_TO_CLASSPATH %%G
popd
goto CLASSPATH_END

: APPEND_TO_CLASSPATH
set filename=%~1
set suffix=%filename:~-4%
if %suffix% equ .jar set CLASSPATH=%CLASSPATH%;%KARAF_HOME%\lib\%filename%
goto :EOF

:CLASSPATH_END

rem Execute the JVM or the load the profiler
if "%KARAF_PROFILER%" == "" goto :RUN
    rem Execute the profiler if it has been configured
    call :warn Loading profiler script: %KARAF_PROFILER_SCRIPT%
    call %KARAF_PROFILER_SCRIPT%

:RUN
    SET OPTS=-Dkaraf.startLocalConsole=true -Dkaraf.startRemoteShell=true
    SET MAIN=org.apache.karaf.main.Main
    SET SHIFT=false

:RUN_LOOP
    if "%1" == "stop" goto :EXECUTE_STOP
    if "%1" == "console" goto :EXECUTE_CONSOLE
    if "%1" == "server" goto :EXECUTE_SERVER
    if "%1" == "client" goto :EXECUTE_CLIENT
    if "%1" == "clean" goto :EXECUTE_CLEAN
    if "%1" == "debug" goto :EXECUTE_DEBUG
    goto :EXECUTE

:EXECUTE_STOP
    SET MAIN=org.apache.karaf.main.Stop
    shift
    goto :RUN_LOOP

:EXECUTE_CONSOLE
    shift
    goto :RUN_LOOP

:EXECUTE_SERVER
    SET OPTS=-Dkaraf.startLocalConsole=false -Dkaraf.startRemoteShell=true
    shift
    goto :RUN_LOOP

:EXECUTE_CLIENT
    SET OPTS=-Dkaraf.startLocalConsole=true -Dkaraf.startRemoteShell=false
    shift
    goto :RUN_LOOP

:EXECUTE_CLEAN
    rmdir /S /Q %KARAF_DATA%
    shift
    goto :RUN_LOOP

:EXECUTE_DEBUG
    if "%JAVA_DEBUG_OPTS%" == "" set JAVA_DEBUG_OPTS=%DEFAULT_JAVA_DEBUG_OPTS%
    set "JAVA_OPTS=%JAVA_DEBUG_OPTS% %JAVA_OPTS%"
    shift
    goto :RUN_LOOP

:EXECUTE
    SET ARGS=%1 %2 %3 %4 %5 %6 %7 %8
    rem Execute the Java Virtual Machine
    cd %KARAF_BASE%
    "%JAVA%" %JAVA_OPTS% %OPTS% -classpath "%CLASSPATH%" -Djava.endorsed.dirs="%JAVA_HOME%\jre\lib\endorsed;%JAVA_HOME%\lib\endorsed;%KARAF_HOME%\lib\endorsed" -Djava.ext.dirs="%JAVA_HOME%\jre\lib\ext;%JAVA_HOME%\lib\ext;%KARAF_HOME%\lib\ext" -Dkaraf.instances="%KARAF_HOME%\instances" -Dkaraf.home="%KARAF_HOME%" -Dkaraf.base="%KARAF_BASE%" -Djava.io.tmpdir="%KARAF_DATA%\tmp" -Dkaraf.data="%KARAF_DATA%" -Djava.util.logging.config.file="%KARAF_BASE%\etc\java.util.logging.properties" %KARAF_OPTS% %MAIN% %ARGS%

rem # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

:END

endlocal

if not "%PAUSE%" == "" pause

:END_NO_PAUSE

