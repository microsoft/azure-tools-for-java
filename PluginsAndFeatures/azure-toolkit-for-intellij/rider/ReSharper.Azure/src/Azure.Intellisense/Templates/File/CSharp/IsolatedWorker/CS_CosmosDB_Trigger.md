---
guid: b04cdc48-da71-431e-9933-e56fdd8a3022
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=CosmosDbTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), DATABASEVALUE, COLLECTIONVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
DATABASEVALUE-expression: constant("databaseName")
COLLECTIONVALUE-expression: constant("collectionName")
CONNECTIONVALUE-expression: constant("")
---

# CosmosDB Trigger

```
$HEADER$using System;
using System.Collections.Generic;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [Function("$CLASS$")]
        public static void Run([CosmosDBTrigger(
            databaseName: "$DATABASEVALUE$",
            collectionName: "$COLLECTIONVALUE$",
            ConnectionStringSetting = "$CONNECTIONVALUE$",
            LeaseCollectionName = "leases")] IReadOnlyList<MyDocument> input, FunctionContext context)
        {
            var logger = context.GetLogger("$CLASS$");
            if (input != null && input.Count > 0)
            {
                logger.LogInformation("Documents modified: " + input.Count);
                logger.LogInformation("First document Id: " + input[0].Id);
            }$END$
        }
    }

    public class MyDocument
    {
        public string Id { get; set; }

        public string Text { get; set; }

        public int Number { get; set; }

        public bool Boolean { get; set; }
    }
}
```
