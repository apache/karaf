#!/bin/sh

GAV=${1}

if [ ! -f "${GAV}.jar" ]
then
 echo "Usage: stage_existing_artifact.sh <group.artifact-version>"
 exit
fi

################################################################################
#                DEPLOY FUNCTION:  deploy [type] [classifier]
################################################################################

deploy() {
 EXT=${1:-jar}
 TARGET=${GAV}${2:+-$2}.${EXT}
 CLSFR=${2:+-Dclassifier=$2}

 # upload artifact
 mvn deploy:deploy-file \
  -DrepositoryId=apache.releases.https \
  -Durl=https://repository.apache.org/service/local/staging/deploy/maven2 \
  -DpomFile=${GAV}.pom -Dpackaging=${EXT} -Dfile=${TARGET} ${CLSFR}

 # upload signature
 mvn deploy:deploy-file \
  -DrepositoryId=apache.releases.https \
  -Durl=https://repository.apache.org/service/local/staging/deploy/maven2 \
  -DpomFile=${GAV}.pom -Dpackaging=${EXT}.asc -Dfile=${TARGET}.asc ${CLSFR}
}

echo "################################################################################"
echo "                             STAGE PRIMARY ARTIFACTS                            "
echo "################################################################################"

deploy pom
deploy jar

deploy jar sources

echo "################################################################################"
echo "                             STAGE BINARY ASSEMBLIES                            "
echo "################################################################################"

deploy zip bin
deploy tar.gz bin

echo "################################################################################"
echo "                            STAGE PROJECT ASSEMBLIES                           "
echo "################################################################################"

deploy zip project
deploy tar.gz project

