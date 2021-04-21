## Domain Concepts

* `Resource` represents a Azure resource (or a intellij module), which can be consumed by other resources
  or consume other resources.
  * intellij module can only consume other resources
* `Connection` represents the consumption relation between 2 Resources, of which, one is the `resource` to be
  consumed and the other is the `consumer` which consumes the `resource`.

both `resource` and `consumer` are resources

## Requirements

* basically, most resource connections involves at least these 2 tasks:
  * configure the resource about **who can consume me and how I can be consumed**. e.g.
    * configure mysql database to allow access from a certain IP/range.

  * configure the consumer about **which to consume and how to consume**. e.g.
    * setup environment variables about `jdbcurl`/`username`/`password` for local java app.
    * configure application settings/connection strings(finally set as environment variables) about `jdbcurl`
      /`username`/`password` for azure appservice.

### Considerations

* Azure resource related information/configuration(resource id, username...) are user data and should be private.
  * private data should be stored in environment variables and referred via environment variables in local
    run program.(this is also how `Application Settings` and `Connection Strings` work in Azure App Service)
  * information of resources should be persisted outside the project/workspace. It would be otherwise dangerous
    if user mistakenly push them to public platforms like GitHub.
  * environment variables can be setup automatically by the plugin/resource connector since we masters all the
    required information
* Resource related password/token/... should be especially securely managed.
  * use Intellij `SafeStore` to store passwords.
* Created connections can be easily shared among developers contributing to the same project.
  * connections should be persisted inside project/workspace so that they can be tracked by git.
* Creation/configuration of different resource connection types should share consistent UI/UX.
  * general `AzureResourceConnetor` + UI Guideline + common UI components.
* Probable requirement of compatibility with `Azure Resource Connector service`

## Framework

### interface `Connection` & `ConnectionDefinition<R extends Resource, C extends Resource>`

`Connection` is a composition of 2 resources, of which, one is `resource`, and the other is
`consumer`(to consumer the `resource`).
`ConnectionDefinition` defines how to persist/load, consume a type of resource connections.

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
  /**
   * @return the resource consumed by consumer
   */
  R getResource();

  /**
   * @return the consumer consuming resource
   */
  C getConsumer();

  /**
   * is this connection applicable for the specified {@code configuration}.<br>
   * - the {@code Connect Azure Resource} before run task will take effect if
   * applicable: the {@link #prepareBeforeRun} & {@link #updateJavaParametersAtRun}
   * will be called.
   *
   * @return true if this connection should intervene the specified {@code configuration}.
   */
  default boolean isApplicableFor(@NotNull RunConfiguration configuration) {
    return false;
  }

  /**
   * do some preparation in the {@code Connect Azure Resource} before run task
   * of the {@code configuration}<br>
   */
  boolean prepareBeforeRun(@NotNull RunConfiguration configuration, DataContext dataContext);

  /**
   * update java parameters exactly before start the {@code configuration}
   */
  default void updateJavaParametersAtRun(RunConfiguration configuration, @NotNull JavaParameters parameters) {
  }
}
```

```java
public interface ConnectionDefinition<R extends Resource, C extends Resource> {
  /**
   * create {@link Connection} from given {@code resource} and {@code consumer}
   */
  @Nonnull
  Connection<R, C> create(R resource, C consumer);

  /**
   * read/deserialize a instance of {@link Connection} from {@code element}
   */
  @Nullable
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
   *
   * @return false if the give {@code connection} is not valid and should not
   * be created and persisted.
   */
  boolean validate(Connection<R, C> connection, Project project);

  /**
   * are the connections defined by this definition generally applicable for
   * the specified {@code configuration}.<br>
   * - a {@code Connect Azure Resource} before run task will be added to the
   * RC if applicable.<br> but the before run task will take no effect if
   * there are no connections applicable for the specified {@code configuration}.
   */
  default boolean isApplicableFor(@NotNull RunConfiguration configuration) {
    return false;
  }

  /**
   * get <b>custom</b> connector dialog to create resource connection of
   * a type defined by this definition
   */
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
/**
 * the <b>{@code resource}</b> in <b>{@code resource connection}</b><br>
 * it's usually An Azure resource or an intellij module
 */
public interface Resource {
  String FIELD_TYPE = "type";
  String FIELD_ID = "id";

  String getType();

  /**
   * get the id of the resource<br>
   * be careful <b>NOT</b> to return the Azure resource id directly since
   * this id will be saved somewhere in the workspace and may be tracked by git.<br>
   * a good practice would be returning the hashed(e.g. md5/sha1/sha256...) Azure resource id
   */
  String getId();
}
```
```java
public interface ResourceDefinition<T extends Resource> {
    int RESOURCE = 1;
    int CONSUMER = 2;
    int BOTH = RESOURCE | CONSUMER;

    /**
     * get the role of the resource
     *
     * @return {@link ResourceDefinition#RESOURCE RESOURCE=1} if this resource can only be consumed,<br>
     * {@link ResourceDefinition#CONSUMER CONSUMER=2} if this resource can only be a consumer or<br>
     * {@link ResourceDefinition#BOTH BOTH=3} if this resource can be both resource and consumer
     */
    default int getRole() {
        return RESOURCE;
    }

    default String getTitle() {
        return this.getType();
    }

    String getType();

    /**
     * get resource selection panel<br>
     * with this panel, user could select/create a {@link T} resource.
     *
     * @param type type of the resource
     */
    AzureFormJPanel<T> getResourcesPanel(@Nonnull final String type, final Project project);

    /**
     * write/serialize {@code resouce} to {@code element} for persistence
     *
     * @return true if to persist, false otherwise
     */
    boolean write(@Nonnull final Element element, @Nonnull final T resouce);

    /**
     * read/deserialize a instance of {@link T} from {@code element}
     */
    T read(@Nonnull final Element element);
}
```
### class `ConnectionManager` & `ResourceManager`

`ConnectionManager` manages
* existed resource connections
* registered resource connection definitions

`ResourceManager` manages
* existed resources
* registered resource definitions

### class `ConnectorDialog`

`ConnectorDialog` is the UI to create(or manage existing resource connections in the future) a resource connection.

User would be able to open the dialog from the context menu on a module(in Project Explorer) or azure resource(in Azure Explorer)

the dialog consists of 3 parts:
_**consumer selector**_ (user can only select the modules of the current project for now)
_**resource type selector**_ (user can only select the modules of the current project for now)
_**resource details panel**_ (allows user to select the type of resources and the resource.).

## How to and what happens at background

1. implement `Resource` & `ResourceDefinition`

2. implement `Connection` & `ConnectionDefinition`when needed

3. register a resource type to `ConnectionManager`

```java
ResourceManager.registerDefinition(ModuleResource.Definition.IJ_MODULE);
ResourceManager.registerDefinition(MySQLDatabaseResource.Definition.AZURE_MYSQL);
final String resourceType = MySQLDatabaseResource.TYPE;
final String consumerType = ModuleResource.TYPE;
ConnectionManager.registerDefinition(resourceType, consumerType, MySQLDatabaseResourceConnection.Definition.MODULE_MYSQL);
```
4. open dialog on user actions

```java
final ConnectorDialog dialog=new ConnectorDialog();
dialog.setConsumer(xxx);
//dialog.setResource(xxx);
dialog.show();
```

* **_consumer selector_** lists all the modules of the current project.
* **_resource types selector_** lists all the registered resource types
* the corresponding **_resource details panel_**(get from `ResourceDefinition`) shows when user selector a resource type;

5. user click ok to add resource connection