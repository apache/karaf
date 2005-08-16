@echo off

rem
rem The following 'set' command should be automatically
rem assigned during install, if not, edit it to reflect
rem your Java installation.
rem

set JAVA_HOME="%%Java directory%%"

rem
rem You do not need to edit the following.
rem

%JAVA_HOME%\bin\java -jar lib\felix.jar

