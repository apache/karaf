#!/bin/sh

VERSION=$1

if [ "$VERSION" = "" ]
then

  echo "Usage: $0 <new-bundleplugin-version>"
  exit

fi

cd ../pom

for POM in `find .. -name .svn -prune -o -name pom.xml -print`
do

  mvn "-DprojectFile=$POM" \
      "-Dxpath=/project/build/plugins/plugin[artifactId[.='maven-bundle-plugin']]/version" \
      "-DnewValue=$VERSION" \
      org.apache.maven.plugins:maven-pom-plugin:1.0-SNAPSHOT:alter-by-xpath

  mvn "-DprojectFile=$POM" \
      "-DskipXPath=/project/build/plugins/plugin[artifactId[.='maven-bundle-plugin']]/version" \
      "-Dxpath=/project/build/plugins/plugin[artifactId[.='maven-bundle-plugin']]" \
      "-DnewElement=version" "-DnewValue=$VERSION" \
      org.apache.maven.plugins:maven-pom-plugin:1.0-SNAPSHOT:add-by-xpath

done

