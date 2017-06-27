#!/bin/bash

set -e

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
cd $SCRIPTPATH


VERSION="3.0.4-SNAPSHOT"
MAVEN_QUIET=""
INTELLIJ_VERSION=false

while getopts ":hqv" option; do
    case $option in
        h) echo "usage: $0 [-h] [-q] [-v]"; exit ;;
        q) MAVEN_QUIET="-q" ;;
        v) INTELLIJ_VERSION=true ;;
        ?) echo "error: option -$OPTARG is not implemented"; exit ;;
    esac
done


ECLIPSE_KEY=${ECLIPSE_TELKEY}
INTELLIJ_KEY=${INTELLIJ_TELKEY}
ARTIFACTS_DIR="artifacts"
# check dir exists
if [ ! -d  "$ARTIFACTS_DIR" ]; then
    echo "Creating artifacts directory $ARTIFACTS_DIR"
    mkdir -p $ARTIFACTS_DIR
fi

# echo shell commands when they are executed.
set -x

# Build Utils
mvn install -f $SCRIPTPATH/Utils/pom.xml -Dmaven.repo.local=$SCRIPTPATH/.repository $MAVEN_QUIET
rm -rf /c/Signing/ToSign/*
cp $SCRIPTPATH/Utils/azuretools-core/target/azuretools-core-${VERSION}.jar /c/Signing/ToSign/
cp $SCRIPTPATH/Utils/azure-explorer-common/target/azure-explorer-common-${VERSION}.jar /c/Signing/ToSign/
cp $SCRIPTPATH/Utils/hdinsight-node-common/target/hdinsight-node-common-${VERSION}.jar /c/Signing/ToSign/

if /c/Signing/CodeSignUtility/csu.exe -c1=170
then
	echo jar signed successfully
else
	echo ERROR: sign task failed
	exit 1
fi

cp /c/Signing/Signed/azuretools-core-${VERSION}.jar $SCRIPTPATH/Utils/azuretools-core/target/azuretools-core-${VERSION}.jar
cp /c/Signing/Signed/azure-explorer-common-${VERSION}.jar $SCRIPTPATH/Utils/azure-explorer-common/target/azure-explorer-common-${VERSION}.jar
cp /c/Signing/Signed/hdinsight-node-common-${VERSION}.jar $SCRIPTPATH/Utils/hdinsight-node-common/target/hdinsight-node-common-${VERSION}.jar 

mvn install:install-file -Dfile=$SCRIPTPATH/Utils/azuretools-core/target/azuretools-core-${VERSION}.jar -DgroupId=com.microsoft.azuretools -DartifactId=azuretools-core -Dversion=${VERSION} -Dpackaging=jar -Dmaven.repo.local=$SCRIPTPATH/.repository
mvn install:install-file -Dfile=$SCRIPTPATH/Utils/azure-explorer-common/target/azure-explorer-common-${VERSION}.jar -DgroupId=com.microsoft.azuretools -DartifactId=azure-explorer-common -Dversion=${VERSION} -Dpackaging=jar -Dmaven.repo.local=$SCRIPTPATH/.repository
mvn install:install-file -Dfile=$SCRIPTPATH/Utils/hdinsight-node-common/target/hdinsight-node-common-${VERSION}.jar -DgroupId=com.microsoft.azuretools -DartifactId=hdinsight-node-common -Dversion=${VERSION} -Dpackaging=jar -Dmaven.repo.local=$SCRIPTPATH/.repository 

mvn install -f $SCRIPTPATH/PluginsAndFeatures/AddLibrary/AzureLibraries/pom.xml -Dmaven.repo.local=$SCRIPTPATH/.repository $MAVEN_QUIET

# # Build eclipse plugin
mvn clean install -f $SCRIPTPATH/PluginsAndFeatures/azure-toolkit-for-eclipse/pom.xml -Dinstrkey=${ECLIPSE_KEY}  $MAVEN_QUIET
cp ./PluginsAndFeatures/azure-toolkit-for-eclipse/WindowsAzurePlugin4EJ/target/WindowsAzurePlugin4EJ*.zip ./$ARTIFACTS_DIR/WindowsAzurePlugin4EJ.zip

chmod +x ./gradlew
chmod +x ./tools/IntellijVersionHelper

# Build intellij 2016 plugin
if [ $INTELLIJ_VERSION == "true" ] ; then
    ./tools/IntellijVersionHelper 2016.3
fi

./gradlew clean buildPlugin --project-dir ./PluginsAndFeatures/azure-toolkit-for-intellij -s -Papplicationinsights.key=${INTELLIJ_KEY} -Pintellij_version=IU-2016.3 -Pdep_plugins=org.intellij.scala:2016.3.5

cp ./PluginsAndFeatures/azure-toolkit-for-intellij/build/distributions/azure-toolkit-for-intellij.zip ./$ARTIFACTS_DIR/azure-toolkit-for-intellij-2016.zip

# Build intellij 2017 plugin
if [ $INTELLIJ_VERSION == "true" ] ; then
    ./tools/IntellijVersionHelper 2017.1
fi
./gradlew clean buildPlugin --project-dir ./PluginsAndFeatures/azure-toolkit-for-intellij -s -Papplicationinsights.key=${INTELLIJ_KEY}
cp ./PluginsAndFeatures/azure-toolkit-for-intellij/build/distributions/azure-toolkit-for-intellij.zip ./$ARTIFACTS_DIR/azure-toolkit-for-intellij-2017.zip

# Extract jars to sign
# intelliJ
rm -rf /c/jenkins/toSignPackage/intelliJ/*
unzip -p $SCRIPTPATH/artifacts/azure-toolkit-for-intellij-2016.zip azure-toolkit-for-intellij/lib/azure-toolkit-for-intellij.jar > /c/jenkins/toSignPackage/intelliJ/azure-toolkit-for-intellij_2016.jar
unzip -p $SCRIPTPATH/artifacts/azure-toolkit-for-intellij-2017.zip azure-toolkit-for-intellij/lib/azure-toolkit-for-intellij.jar > /c/jenkins/toSignPackage/intelliJ/azure-toolkit-for-intellij_2017.jar

# Eclipse
rm -rf /c/jenkins/toSignPackage/eclipse/*
unzip -j artifacts/WindowsAzurePlugin4EJ.zip "**/*.jar" "*.jar" -d /c/jenkins/toSignPackage/eclipse

echo "ALL BUILD SUCCESSFUL"
