#!/bin/sh


echo "cd into felix directory"
cd ../main

echo "lauching felix with core gateway properties"
java -Dfelix.config.properties=file:../mosgi.doc/config.properties.core -jar bin/felix.jar

