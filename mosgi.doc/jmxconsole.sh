#!/bin/sh
echo "cd into felix directory"
cd ../org.apache.felix.main

echo "lauching felix with jmxconsole properties"
java -Dfelix.config.properties=file:../mosgi.doc/config.properties.jmxconsole -jar bin/felix.jar
