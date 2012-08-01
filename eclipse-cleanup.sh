#!/bin/bash

find . -name .project   -exec rm -f '{}' \;
find . -name .classpath -exec rm -f '{}' \;
find . -name .settings  -exec rm -f -r '{}' \;
