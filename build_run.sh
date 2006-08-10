#!/bin/sh

mvn clean install; 
cd main/ ; 
java -jar bin/felix.jar ; 
cd ../ 
