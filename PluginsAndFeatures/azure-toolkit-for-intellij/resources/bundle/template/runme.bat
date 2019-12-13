@ECHO OFF

SET OPT=%cd%\\opt
SET "HADOOP_HOME=%OPT%\\${new File(winutilsDir).name}"
SET ADA_PROVIDER=AadProvider.json
SET AZURE_TOOLS_CONFIG_DIR=%userprofile%\\AzureToolsForIntelliJ
:: SET JDK8_HOME=%OPT%\\jdk8
SET JDK8_INSTALL_HOME=%OPT%\\${new File(jdkDir).name}
SET SCALA211_HOME=%OPT%\\${new File(scalaSdkDir).name}
SET IDEA_PROPERTIES=%IDE_BIN_DIR%\\idea.properties
SET IDEA_VM_OPTIONS=%IDE_BIN_DIR%\\win\\idea%BITS%.exe.vmoptions
:: SET SCALA211_INSTALL_HOME=%OPT%\\scala-2.11.12-win

if EXIST %ADA_PROVIDER% (
	md "%AZURE_TOOLS_CONFIG_DIR%"
	copy %ADA_PROVIDER% "%AZURE_TOOLS_CONFIG_DIR%\\"
)

:: if not EXIST "%JDK8_HOME%" mklink /J /D "%JDK8_HOME%" "%JDK8_INSTALL_HOME%"
:: if not EXIST "%SCALA211_HOME%" mklink /J /D "%SCALA211_HOME%" "%SCALA211_INSTALL_HOME%"

bin\\idea.bat

