@echo off
git checkout -- ../PluginsAndFeatures/azure-toolkit-for-intellij/resources/META-INF/plugin.xml
javac IntellijVersionHelper.java
java IntellijVersionHelper %1
exit %ERRORLEVEL%