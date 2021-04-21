---
guid: e3039c5d-886c-4cde-9bbc-9e1155d879ee
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
---

# funccl

Creates an orchestration client function method

```

[FunctionName(nameof($OrchestrationClientClassName$))]
public async Task Run(
    [$TriggerAttribute$] $ParameterType$ $parameterName$,
    [DurableClient] IDurableClient client,
    ILogger logger)
{
    $END$
    // TODO
    // object input = ;
    // string instanceId = await client.StartNewAsync(
    //    nameof(OrchestrationClassName),
    //    input);
}
```
