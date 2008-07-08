#!/bin/sh


cd ../../main

echo "lauching Felix with UPnP bundles"
java -Dfelix.config.properties=file:../upnp/doc/config.properties.upnp -jar bin/felix.jar

