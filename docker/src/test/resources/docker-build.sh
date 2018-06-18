#!/bin/bash
docker build --cache-from openjdk:8-jre --tag karaf/karaf-docker:4.2.0 .