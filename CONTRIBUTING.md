## Azure Toolkit for IntelliJ IDEA
### Building
* Clone the repository with HTTPS or SSH:
    ```
    $ git clone https://github.com/Microsoft/azure-tools-for-java.git
    $ cd azure-tools-for-java
    ```
* Run the following command under the project base path:
    ```
    $ mvn clean install -f Utils/pom.xml
    ```
* Use Gradle to build the plugin
    ```
    $ cd PluginsAndFeatures/azure-toolkit-for-intellij
    $ ./gradlew buildPlugin
    ```
    You can find the outputs under ```PluginsAndFeatures/azure-toolkit-for-intellij/build/distributions```
    
### Run/Debug
* Open IntelliJ, open PluginsAndFeatures/azure-toolkit-for-intellij.
* Run/Debug the plugin by triggering the Gradle task as following:
    ![intellij_run_debug](docs/resources/intellij_run_debug.png)

## Azure Toolkit for Eclipse
### Building
* Clone the repository with HTTPS or SSH:
    ```
    $ git clone https://github.com/Microsoft/azure-tools-for-java.git
    $ cd azure-tools-for-java
    ```
* Run the following command under the project base path:
    ```
    $ mvn clean install -f Utils/pom.xml
    $ mvn clean install -f PluginsAndFeatures/AddLibrary/AzureLibraries/pom.xml
    ```
* Use Maven to build the plugin
    ```
    mvn clean install -f PluginsAndFeatures/azure-toolkit-for-eclipse/pom.xml
    ```
    You can find the outputs under ```PluginsAndFeatures/azure-toolkit-for-eclipse/WindowsAzurePlugin4EJ/target```

### Run/Debug
* Open Eclipse, select ```import > Maven > Existing Maven Projects```:
    ![eclipse_import_step1](docs/resources/eclipse_import_step1.png)
* Import all the modules under ```PluginsAndFeatures/azure-toolkit-for-eclipse```:
    ![eclipse_import_step2](docs/resources/eclipse_import_step2.png)
* New a run/debug configuration and click Run/Debug:
    ![eclipse_debug](docs/resources/eclipse_debug.png)
