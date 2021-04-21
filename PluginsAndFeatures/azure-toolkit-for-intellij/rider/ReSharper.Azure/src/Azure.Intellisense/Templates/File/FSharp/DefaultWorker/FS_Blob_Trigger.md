---
guid: 3e3ef753-81d7-4130-a8c9-aff5cabc23ed
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=BlobTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsDefaultWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("samples-workitems")
CONNECTIONVALUE-expression: constant("")
---

# Blob Trigger

```
namespace $NAMESPACE$

open System.IO
open Microsoft.Azure.WebJobs
open Microsoft.Azure.WebJobs.Host
open Microsoft.Extensions.Logging

module $CLASS$ =
    [<FunctionName("$CLASS$")>]
    let run ([<BlobTrigger("$PATHVALUE$/{name}", Connection = "$CONNECTIONVALUE$")>] myBlob: Stream, name: string, log: ILogger) =
        let msg = sprintf "F# Blob trigger function Processed blob\nName: %s \n Size: %d Bytes" name myBlob.Length
        log.LogInformation msg$END$
```
