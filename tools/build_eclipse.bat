@ECHO OFF
REM --------------------------------------------------------------------------------------------
REM Copyright (c) Microsoft Corporation. All rights reserved.
REM Licensed under the MIT License. See License.txt in the project root for license information.
REM --------------------------------------------------------------------------------------------

CD %~dp0\..

CALL mvn -v
IF NOT %ERRORLEVEL% EQU 0 ECHO Maven not found. Please install Maven first. && EXIT /b 1

CALL :MAVEN_BUILD common-utils .\Utils
IF NOT %ERRORLEVEL% EQU 0 GOTO EOF

CALL :MAVEN_BUILD libraries-for-azuretools-sdk .\PluginsAndFeatures\AddLibrary\AzureLibraries
IF NOT %ERRORLEVEL% EQU 0 GOTO EOF

CALL :MAVEN_BUILD azure-toolkit-for-eclipse .\PluginsAndFeatures\azure-toolkit-for-eclipse
IF NOT %ERRORLEVEL% EQU 0 GOTO EOF

GOTO SUCCESS

:MAVEN_BUILD
ECHO -------------------------------------------------------------------------------
ECHO building %1
ECHO -------------------------------------------------------------------------------
CALL mvn clean install -f %2
IF NOT %ERRORLEVEL% EQU 0 ECHO Fail to build %1... && EXIT /b 1
GOTO EOF

:SUCCESS
ECHO -------------------------------------------------------------------------------
ECHO ALL BUILD SUCCESS
ECHO -------------------------------------------------------------------------------
GOTO EOF

:EOF
