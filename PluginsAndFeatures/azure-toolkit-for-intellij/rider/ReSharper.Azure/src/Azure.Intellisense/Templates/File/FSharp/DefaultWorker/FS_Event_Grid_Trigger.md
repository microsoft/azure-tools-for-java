---
guid: 4c32fa2b-ec21-4789-ba43-b5a897fb8f5b
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=EventGridTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsDefaultWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
---

# Event Grid Trigger

```
namespace $NAMESPACE$
// Default URL for triggering event grid function in the local environment.
// http://localhost:7071/runtime/webhooks/EventGrid?functionName={functionname}

open Microsoft.Azure.WebJobs
open Microsoft.Azure.WebJobs.Host
open Microsoft.Azure.EventGrid.Models
open Microsoft.Azure.WebJobs.Extensions.EventGrid
open Microsoft.Extensions.Logging

module $CLASS$ =
    [<FunctionName("$CLASS$")>]
    let run ([<EventGridTrigger>] eventGridEvent: EventGridEvent, log: ILogger) =
        log.LogInformation(eventGridEvent.Data.ToString())$END$
```
