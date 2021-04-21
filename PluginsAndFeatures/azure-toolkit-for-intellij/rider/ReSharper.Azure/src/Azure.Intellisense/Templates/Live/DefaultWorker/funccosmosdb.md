---
guid: 9231f311-94ee-4170-a2ce-db05af3c12b6
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS, DATABASEVALUE, COLLECTIONVALUE, (CONNECTIONVALUE)
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
DATABASEVALUE-expression: constant("databaseName")
COLLECTIONVALUE-expression: constant("collectionName")
---

# funccosmosdb

Creates an Azure Function method with a CosmosDB trigger.

```
[FunctionName("$CLASS$")]
public static async Task RunAsync([CosmosDBTrigger(
    databaseName: "$DATABASEVALUE$",
    collectionName: "$COLLECTIONVALUE$",
    ConnectionStringSetting = "$CONNECTIONVALUE$",
    LeaseCollectionName = "leases")]IReadOnlyList<Document> input, ILogger log)
{
    if (input != null && input.Count > 0)
    {
        log.LogInformation("Documents modified " + input.Count);
        log.LogInformation("First document Id " + input[0].Id);$END$
    }
}
```
