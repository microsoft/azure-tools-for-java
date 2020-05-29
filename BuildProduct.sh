#!/bin/bash

set -e

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
cd "$SCRIPTPATH"

# Utils
VERSION="3.38.0"
MAVEN_QUIET=""

# Eclipse
BUILD_ECLIPSE=true

# IntelliJ
BUILD_INTELLIJ=true

INJECT_INTELLIJ_VERSION=false

IJ_VERSION_LATEST=2020.1
IJ_DISPLAY_VERSION_LATEST=2020.1
IJ_SCALA_VERSION_LATEST=2020.1.25

while getopts "hqve:" option; do
    case $option in
        h) echo "usage: $0 [-h] [-q] [-v] [-e eclipse/intellij]"; exit ;;
        q) MAVEN_QUIET="-q" ;;
        v) INJECT_INTELLIJ_VERSION=true ;;
        e)
          shopt -s nocasematch
          case $OPTARG in 
            eclipse) BUILD_ECLIPSE=false ;;
            intellij) BUILD_INTELLIJ=false ;;
          esac ;;
        ?) echo "error: option -$OPTARG is not implemented"; exit ;;
    esac
done


ECLIPSE_KEY=${ECLIPSE_TELKEY}
INTELLIJ_KEY=${INTELLIJ_TELKEY}
ARTIFACTS_DIR="artifacts"
TOSIGNPATH="/c/Signing/ToSign"
SIGNEDPATH="/c/Signing/Signed"
ECLIPSE_TOSIGN="/c/jenkins/toSignPackage/eclipse"
INTELLIJ_TOSIGN="/c/jenkins/toSignPackage/intelliJ"

# check dir exists
if [ ! -d  "$ARTIFACTS_DIR" ]; then
    echo "Creating artifacts directory $ARTIFACTS_DIR"
    mkdir -p $ARTIFACTS_DIR
fi

# echo shell commands when they are executed.
set -x

# Build Utils
echo "Building Utils ..."
mvn install -f ./Utils/pom.xml -Dmaven.repo.local=./.repository $MAVEN_QUIET
rm -rf ${TOSIGNPATH}/*
cp ./Utils/azuretools-core/target/azuretools-core-${VERSION}.jar ${TOSIGNPATH}/
cp ./Utils/azure-explorer-common/target/azure-explorer-common-${VERSION}.jar ${TOSIGNPATH}/
cp ./Utils/hdinsight-node-common/target/hdinsight-node-common-${VERSION}.jar ${TOSIGNPATH}/

if node /c/Signing/ESRP/signScript/index.js
then
	echo jar signed successfully
else
	echo ERROR: sign task failed
	exit 1
fi

# Continue building Utils with signed jars
cp ${SIGNEDPATH}/azuretools-core-${VERSION}.jar ./Utils/azuretools-core/target/azuretools-core-${VERSION}.jar
cp ${SIGNEDPATH}/azure-explorer-common-${VERSION}.jar ./Utils/azure-explorer-common/target/azure-explorer-common-${VERSION}.jar
cp ${SIGNEDPATH}/hdinsight-node-common-${VERSION}.jar ./Utils/hdinsight-node-common/target/hdinsight-node-common-${VERSION}.jar

mvn install:install-file -Dfile=./Utils/azuretools-core/target/azuretools-core-${VERSION}.jar -DgroupId=com.microsoft.azuretools -DartifactId=azuretools-core -Dversion=${VERSION} -Dpackaging=jar -Dmaven.repo.local=./.repository
mvn install:install-file -Dfile=./Utils/azure-explorer-common/target/azure-explorer-common-${VERSION}.jar -DgroupId=com.microsoft.azuretools -DartifactId=azure-explorer-common -Dversion=${VERSION} -Dpackaging=jar -Dmaven.repo.local=./.repository
mvn install:install-file -Dfile=./Utils/hdinsight-node-common/target/hdinsight-node-common-${VERSION}.jar -DgroupId=com.microsoft.azuretools -DartifactId=hdinsight-node-common -Dversion=${VERSION} -Dpackaging=jar -Dmaven.repo.local=./.repository

mvn install -f ./PluginsAndFeatures/AddLibrary/AzureLibraries/pom.xml -Dmaven.repo.local=./.repository $MAVEN_QUIET

# Build Eclipse plugin
if $BUILD_ECLIPSE; then
  echo "Building Eclipse plugin ..."
  mvn clean install -f ./PluginsAndFeatures/azure-toolkit-for-eclipse/pom.xml -Dinstrkey=${ECLIPSE_KEY}  $MAVEN_QUIET
  cp ./PluginsAndFeatures/azure-toolkit-for-eclipse/WindowsAzurePlugin4EJ/target/WindowsAzurePlugin4EJ*.zip ./$ARTIFACTS_DIR/WindowsAzurePlugin4EJ.zip

   # Extract jars to sign
  rm -rf ${ECLIPSE_TOSIGN}/*
  unzip -j artifacts/WindowsAzurePlugin4EJ.zip "**/*.jar" "*.jar" -d ${ECLIPSE_TOSIGN}
fi

# Build IntelliJ plugin
if $BUILD_INTELLIJ; then
  echo "Building IntelliJ plugin ..."
  chmod +x ./tools/IntellijVersionHelper

  # Build intellij plugin for latest version
  if [ $INJECT_INTELLIJ_VERSION == "true" ] ; then
      ./tools/IntellijVersionHelper $IJ_DISPLAY_VERSION_LATEST
  fi
  (cd PluginsAndFeatures/azure-toolkit-for-intellij && ./gradlew clean buildPlugin -s -Papplicationinsights.key=${INTELLIJ_KEY} -Pintellij_version=IC-$IJ_VERSION_LATEST -Pdep_plugins=org.intellij.scala:$IJ_SCALA_VERSION_LATEST)
  cp ./PluginsAndFeatures/azure-toolkit-for-intellij/build/distributions/azure-toolkit-for-intellij.zip ./$ARTIFACTS_DIR/azure-toolkit-for-intellij-$IJ_DISPLAY_VERSION_LATEST.zip

  # Extract jars to sign
  rm -rf ${INTELLIJ_TOSIGN}/*
  unzip -p ./artifacts/azure-toolkit-for-intellij-$IJ_DISPLAY_VERSION_LATEST.zip azure-toolkit-for-intellij/lib/azure-toolkit-for-intellij.jar > ${INTELLIJ_TOSIGN}/azure-toolkit-for-intellij_$IJ_DISPLAY_VERSION_LATEST.jar
fi

echo "ALL BUILD SUCCESSFUL"
