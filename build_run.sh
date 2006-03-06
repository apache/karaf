#!/bin/sh

mvn clean install; 
cd org.apache.felix.main/ ; 
java -jar bin/felix.jar ; 
cd ../ 
