---
guid: 555928d8-ac13-469a-aad5-5b3ef6ad3dc6
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: OrchestratorClassName
---

# funcor

Creates an orchestrator function method

```

[FunctionName(nameof($OrchestratorClassName$))]
public async Task Run(
    [OrchestrationTrigger] IDurableOrchestrationContext context,
    ILogger logger)
{
    $END$
    // Since the orchestrator code is being replayed many times
    // don't depend on non-deterministic behavior or blocking calls such as:
    // - DateTime.Now (use context.CurrentUtcDateTime instead)
    // - Guid.NewGuid (use context.NewGuid instead)
    // - System.IO
    // - Thread.Sleep/Task.Delay (use context.CreateTimer instead)
    //
    // More info: https://docs.microsoft.com/en-us/azure/azure-functions/durable/durable-functions-code-constraints

    // TODO
    // var input = context.GetInput<T>();
    // var activityResult = await context.CallActivityAsync<string>(
    //    nameof(ActivityClassName),
    //    input));
}
```
