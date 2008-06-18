#!/bin/sh

#rm -rf ~/.felix/core

echo "cd into felix directory"
cd ../../main

echo "lauching felix with core gateway properties"
java -Dfelix.config.properties=file:../mosgi/doc/config.properties.core -jar bin/felix.jar

#LAUCHING COMMAND IF UNDER JDK1.4

#java -Djavax.management.builder.initial=org.apache.felix.mosgi.jmx.agent.mx4j.server.MX4JMBeanServerBuilder -Dfelix.config.properties=file:../mosgi/doc/config.properties.core -classpath /home/sfrenot/.m2/repository/mx4j/mx4j/3.0.2/mx4j-3.0.2.jar:/home/sfrenot/.m2/repository/mx4j/mx4j-remote/3.0.1/mx4j-remote-3.0.1.jar:bin/felix.jar org.apache.felix.main.Main
