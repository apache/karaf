#!/bin/sh

#rm -rf ~/.felix/jmxconsole

echo "cd into felix directory"
cd ../../main

echo "lauching felix with jmxconsole properties"
java -Dfelix.config.properties=file:../mosgi/doc/config.properties.jmxconsole -jar bin/felix.jar

#LAUCHING COMMAND IF under JDK1.4
#java -Dfelix.config.properties=file:../mosgi/doc/config.properties.jmxconsole -Dfelix.config.properties=file:../mosgi/doc/config.properties.jmxconsole -classpath /home/sfrenot/.m2/repository/mx4j/mx4j/3.0.2/mx4j-3.0.2.jar:/home/sfrenot/.m2/repository/mx4j/mx4j-remote/3.0.1/mx4j-remote-3.0.1.jar:bin/felix.jar org.apache.felix.main.Main
