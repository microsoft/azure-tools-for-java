---
guid: 7ae1d45e-28cd-48d2-bbb6-bc92bbd64254
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=BlobTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("samples-workitems")
CONNECTIONVALUE-expression: constant("")
---

# Blob Trigger

```
$HEADER$using System;
using System.IO;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [Function("$CLASS$")]
        public static void Run([BlobTrigger("$PATHVALUE$/{name}", Connection = "$CONNECTIONVALUE$")] string myBlob, string name,
            FunctionContext context)
        {
            var logger = context.GetLogger("BlobTriggerCSharp");
            logger.LogInformation($"C# Blob trigger function Processed blob\n Name: {name} \n Data: {myBlob}");$END$
        }
    }
}
```
