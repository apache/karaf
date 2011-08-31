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
rem 

if not "%ECHO%" == "" echo %ECHO%

setlocal
set DIRNAME=%~dp0%
set PROGNAME=%~nx0%
set ARGS=%*

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
    call :warn KARAF_HOME is not valid: %KARAF_HOME%
    goto END
)

if not "%KARAF_BASE%" == "" (
    if not exist "%KARAF_BASE%" (
       call :warn KARAF_BASE is not valid: %KARAF_BASE%
       goto END
    )
)
if "%KARAF_BASE%" == "" (
  set KARAF_BASE=%KARAF_HOME%
)

set DEFAULT_JAVA_OPTS=
set DEFAULT_JAVA_DEBUG_OPTS=-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005

rem Support for loading native libraries
set PATH=%PATH%;%KARAF_BASE%\lib;%KARAF_HOME%\lib

rem Setup the Java Virtual Machine
if not "%JAVA%" == "" goto :Check_JAVA_END
    set JAVA=java
    if "%JAVA_HOME%" == "" call :warn JAVA_HOME not set; results may vary
    if not "%JAVA_HOME%" == "" set JAVA=%JAVA_HOME%\bin\java
    if not exist "%JAVA_HOME%" (
        call :warn JAVA_HOME is not valid: "%JAVA_HOME%"
        goto END
    )
:Check_JAVA_END

if "%JAVA_OPTS%" == "" set JAVA_OPTS=%DEFAULT_JAVA_OPTS%

if "%KARAF_DEBUG%" == "" goto :KARAF_DEBUG_END
    rem Use the defaults if JAVA_DEBUG_OPTS was not set
    if "%JAVA_DEBUG_OPTS%" == "" set JAVA_DEBUG_OPTS=%DEFAULT_JAVA_DEBUG_OPTS%
    
    set "JAVA_OPTS=%JAVA_DEBUG_OPTS% %JAVA_OPTS%"
    call :warn Enabling Java debug options: %JAVA_DEBUG_OPTS%
:KARAF_DEBUG_END

set CLASSPATH=%KARAF_HOME%\system\org\apache\karaf\shell\org.apache.karaf.shell.console\${project.version}\org.apache.karaf.shell.console-${project.version}.jar
set CLASSPATH=%CLASSPATH%;%KARAF_HOME%\system\org\ops4j\pax\logging\pax-logging-api\${pax.logging.version}\pax-logging-api-${pax.logging.version}.jar

:EXECUTE
    if "%SHIFT%" == "true" SET ARGS=%2 %3 %4 %5 %6 %7 %8
    if not "%SHIFT%" == "true" SET ARGS=%1 %2 %3 %4 %5 %6 %7 %8
    rem Execute the Java Virtual Machine
    "%JAVA%" %JAVA_OPTS% %OPTS% -classpath "%CLASSPATH%" -Dkaraf.instances="%KARAF_HOME%\instances" -Dkaraf.home="%KARAF_HOME%" -Dkaraf.base="%KARAF_BASE%" -Djava.util.logging.config.file="%KARAF_BASE%\etc\java.util.logging.properties" %KARAF_OPTS% org.apache.karaf.shell.console.Main --classpath="%KARAF_HOME%\system" %ARGS%

rem # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

:END

endlocal

