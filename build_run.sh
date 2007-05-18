#!/bin/sh

ant
cd main/ ; 
java -jar bin/felix.jar ; 
cd ../ 
