#!/bin/sh

#
# The following variable should be automatically
# assigned during install, if not, edit it to reflect
# your Java installation.
#

java_home=%%Java directory%%

#
# You don't need to edit the following line
#

exec ${java_home}/bin/java -jar lib/felix.jar
