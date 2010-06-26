@echo off

REM ------------------------------------------------------------------------
REM Licensed to the Apache Software Foundation (ASF) under one or more
REM contributor license agreements.  See the NOTICE file distributed with
REM this work for additional information regarding copyright ownership.
REM The ASF licenses this file to You under the Apache License, Version 2.0
REM (the "License"); you may not use this file except in compliance with
REM the License.  You may obtain a copy of the License at
REM
REM http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing, software
REM distributed under the License is distributed on an "AS IS" BASIS,
REM WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM See the License for the specific language governing permissions and
REM limitations under the License.
REM ------------------------------------------------------------------------

setlocal

set APP_NAME=${name}
set APP_LONG_NAME=${displayName}
set APP_BASE=${karaf.base}

if ""%1"" == ""run"" goto doRun
if ""%1"" == ""install"" goto doInstall
if ""%1"" == ""remove"" goto doRemove

echo Usage:  karaf-service ( commands ... )
echo commands:
echo   run               Start %APP_NAME% in the current console
echo   install           Install %APP_NAME% as a Windows service
echo   remove            Remove the %APP_NAME% Windows service
goto end

:doRun
"%APP_BASE%\bin\%APP_NAME%-wrapper.exe" -c "%APP_BASE%\etc\%APP_NAME%-wrapper.conf"
goto end

:doInstall
"%APP_BASE%\bin\%APP_NAME%-wrapper.exe" -i "%APP_BASE%\etc\%APP_NAME%-wrapper.conf"
goto end

:doRemove
"%APP_BASE%\bin\%APP_NAME%-wrapper.exe" -r "%APP_BASE%\etc\%APP_NAME%-wrapper.conf"
goto end

:end
if not "%PAUSE%" == "" pause
