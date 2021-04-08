## Concepts

### interface `Connection` & `ConnectionDefinition<R extends Resource, C extends Resource>`

`Connection` is a composition of 2 resources, of which, one is `resource`, and the other is
`consumer`(to consumer the `resource`).
`ConnectionDefinition` is the definition for a type of resource connections and tell resource
connector how to serialize/deserialize the connection.

The _**resource**_ can be any Azure resources, but only _**Azure Database for MySQL**_ is supported
for now.

The **_consumer_** can be most Azure resources and _**Intellij Project Module**_, but only _**Intellij 
Project Module**_ is supported for now.

* `MySQLDatabaseResourceConnection` the resource connection representing the consumption relation 
  between a _**Intellij Project Module**_ and _**Azure Database for MySQL**_ database.
```java
/**
 * the <b>{@code resource connection}</b>
 *
 * @param <R> type of the resource consumed by {@link C}
 * @param <C> type of the consumer consuming {@link R},
 *            it can only be {@link ModuleResource} for now({@code v3.52.0})
 * @since 3.52.0
 */
public interface Connection<R extends Resource, C extends Resource> {
  String FIELD_TYPE = "type";

  /**
   * @return the resource consumed by consumer
   */
  R getResource();

  /**
   * @return the consumer consuming resource
   */
  C getConsumer();

  /**
   * called before execute the {@code RunConfiguration} of connected module<br>
   * the connection can intervene the run configuration by e.g. setting environment variables
   */
  void beforeRun(@NotNull RunConfiguration configuration, DataContext dataContext);

  default String getType() {
    return typeOf(this.getResource(), this.getConsumer());
  }

  default void setType(String type) {
    assert StringUtils.equals(getType(), type) : String.format("incompatible resource type \"%s\":\"%s\"", getType(), type);
  }

  /**
   * generate common connection type for the connection between {@code resource} and {@code consumer}
   */
  static String typeOf(Resource resource, Resource consumer) {
    return typeOf(resource.getType(), consumer.getType());
  }

  /**
   * generate common connection type for the connection between {@code resourceDefinition} and {@code consumerType}
   */
  static String typeOf(String resourceDefinition, String consumerType) {
    return String.format("%s:%s", resourceDefinition, consumerType);
  }

}
```

```java
public interface ConnectionDefinition<R extends Resource, C extends Resource> {
  /**
   * create {@link Connection} from given {@code resource} and {@code consumer}
   */
  Connection<R, C> create(R resource, C consumer);

  /**
   * read/deserialize a instance of {@link Connection} from {@code element}
   */
  Connection<R, C> read(Element element);

  /**
   * write/serialize {@code connection} to {@code element} for persistence
   *
   * @return true if to persist, false otherwise
   */
  boolean write(Element element, Connection<? extends R, ? extends C> connection);

  /**
   * validate if the given {@code connection} is valid, e.g. check if
   * the given connection had already been created and persisted.
   * @return false if the give {@code connection} is not valid and should not
   * be created and persisted.
   */
  boolean validate(Connection<R, C> connection, Project project);

  @Nullable
  default AzureDialog<Connection<R, C>> getConnectorDialog() {
    return null;
  }
}
```
### interface `Resource` & `ResourceDefinition<T extends Resource>`

`Resource` is the _**resource**_ that can be consumed by the _**consumer**_ in `Connection`.
`ResourceDefinition` is definition for a specific type of resources and tell the resource connector how to select/serialize/deserialize the resource.

* `ModuleResource`: a **special** _**consumer**_ representing a _**Intellij Project Module**_.
* `MySQLDatabaseResource`: the resource representing a _**Azure Database for MySQL**_ database.
```java
public interface Resource {
  @Nonnull
  String getBizId();

  @Nonnull
  String getType();

  default void setType(String type) {
    assert StringUtils.equals(getType(), type) : String.format("incompatible resource type \"%s\":\"%s\"", getType(), type);
  }

  default String getId() {
    return DigestUtils.md5Hex(this.getBizId());
  }

  interface Helper<T extends Resource> extends Serializer<T> {
    AzureFormPanel<T> getResourcesPanel();

    void serializeInto(Element element, T value);

    T deserializeFrom(Element element);
  }
}
```

### class `ResourceConnectionManager`

`ResourceConnectionManager` manages
* existed resource connections and related resources
* registered resource connection types and resource types

### class `ResourceConnectorDialog`

`ResourceConnectorDialog` is the UI to create(or manage existing resource connections in the future) a resource connection.

User would be able to open the dialog from the context menu on a module(in Project Explorer) or azure resource(in Azure Explorer)

the dialog consists of 3 parts:
_**consumer selector**_ (user can only select the modules of the current project for now)
_**resource type selector**_ (user can only select the modules of the current project for now)
_**resource details panel**_ (allows user to select the type of resources and the resource.).

## How to and what happens at background

1. implement `Resource` & `ResourceDefinition`

2. implement `Connection` & `ConnectionDefinition`when needed

3. register a resource type to `ResourceConnectionManager`

```java
  ResourceConnectionManager.registerResourceType("Microsoft.DBforMySQL",MySQLDatabaseResourceManager.getInstance());
  ResourceConnectionManager.registerResourceType("Microsoft.Web",WebAppResourceHelper.getInstance(),CONSUMER|RESOURCE);
  ResourceConnectionManager.registerResourceType("Jetbrains.IjModule",IntellijModuleResourceHelper.getInstance(),ResourceConnectionManager.CONSUMER);
  ResourceConnectionManager.registerConnectionType("Microsoft.DBforMySQL:Jetbrains.IjModule",MySQLDatabaseResourceConnection.getInstance());
```
4. open dialog on user actions

```java
final ResourceConnectorDialog dialog=new ResourceConnectorDialog();
dialog.setConsumer(xxx);
//dialog.setResource(xxx);
dialog.show();
```

* **_consumer selector_** lists all the modules of the current project.
* **_resource types selector_** lists all the registered resource types
* the corresponding **_resource details panel_**(get from `ResourceDefinition`) shows when user selector a resource type;

5. user click ok to add resource connection
