## Concepts

### interface `ResourceConnection` & `ResourceConnection.Helper<T extends ResourceConnection<? extends Resource, ? extends Resource>>`

`ResourceConnection` is a composition of 2 resources, of which, one is `resource`, and the other is
`consumer`(to consumer the `resource`).
`ResourceConnection.Helper` is definition for a specific type of resource connection and tell resource connector how to serialize/deserialize the connection.

The _**resource**_ can be any Azure resources, but only _**Azure Database for MySQL**_ is supported for now.

The **_consumer_** can be most Azure resources and _**Intellij Project Module**_, but only _**Intellij Project Module**_ is supported for now.

* `MySQLDatabaseResourceConnection` the resource connection representing the consumption relation between a
  _**Intellij Project Module**_ and _**Azure Database for MySQL**_ database.
```java
public interface ResourceConnection<R extends Resource, C extends Resource> {
  R getResource();

  C getConsumer();

  void beforeRun(@NotNull RunConfiguration configuration, DataContext dataContext);

  default String getType() {
    return String.format("%s:%s", this.getResource().getType(), this.getConsumer().getType());
  }

  default void setType(String type) {
    assert StringUtils.equals(getType(), type) : String.format("incompatible resource type \"%s\":\"%s\"", getType(), type);
  }

  interface Helper<T extends ResourceConnection<? extends Resource, ? extends Resource>> {

    T connect(Resource resource, Resource consumer);

    void serializeInto(Element element, T value);

    T deserializeFrom(Element element);
  }
}
```

### interface `Resource` & `Resource.Helper<T extends Resource>`

`Resource` is the _**resource**_ that can be consumed by the _**consumer**_ in `ResourceConnection`.
`Resource.Helper` is definition for a specific type of resources and tell the resource connector how to select/serialize/deserialize the resource.

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

1. implement `Resource` & `Resource.Helper`

2. implement `ResourceConnection` & `ResourceConnection.Helper`when needed

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
* the corresponding **_resource details panel_**(get from `Resource.Helper`) shows when user selector a resource type;

5. user click ok to add resource connection
