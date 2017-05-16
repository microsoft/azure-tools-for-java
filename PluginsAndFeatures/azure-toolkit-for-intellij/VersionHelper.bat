@echo off
git checkout -- resources/META-INF/plugin.xml
javac VersionHelper.java
java VersionHelper %1
exit %ERRORLEVEL%