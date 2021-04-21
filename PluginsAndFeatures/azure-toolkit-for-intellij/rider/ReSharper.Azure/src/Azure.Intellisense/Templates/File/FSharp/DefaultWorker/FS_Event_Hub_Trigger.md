---
guid: 4a3273cb-d595-4bd6-9b69-8eaf71120b55
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=fs, FileName=EventHubTrigger, ValidateFileName=True
scopes: InAzureFunctionsFSharpProject;MustUseAzureFunctionsDefaultWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), PATHVALUE, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
PATHVALUE-expression: constant("samples-workitems")
CONNECTIONVALUE-expression: constant("")
---

# Event Hub Trigger

```
namespace $NAMESPACE$

open System
open System.Text
open System.Threading.Tasks
open Microsoft.Azure.EventHubs
open Microsoft.Azure.WebJobs
open Microsoft.Extensions.Logging

module $CLASS$ =
    [<FunctionName("$CLASS$")>]
    let run([<EventHubTrigger("$PATHVALUE$", Connection = "$CONNECTIONVALUE$")>] events: EventData[], log: ILogger) =
        async {
            let exns = ResizeArray<Exception>()

            for eventData in events do
                try
                    let msgBody = Encoding.UTF8.GetString(eventData.Body.Array, eventData.Body.Offset, eventData.Body.Count)
                    let msg = sprintf "F# Event Hub trigger function processed a message: %s" msgBody
                    log.LogInformation msg
                    Task.Yield() |> Async.AwaitTask
                with
                | e -> exns.Add(e)

            if exns.Count > 1 then
                raise(AggregateException(exns))
            if exns.Count = 1 then
                raise(exns.[0])
        } |> Async.StartAsTask$END$
```
