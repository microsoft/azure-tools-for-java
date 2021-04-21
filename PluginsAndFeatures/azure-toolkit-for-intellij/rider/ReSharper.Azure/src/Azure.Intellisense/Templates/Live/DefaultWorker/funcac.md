---
guid: 581c1130-0496-4418-8ebd-57679251f1b5
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: ActivityClassName, OutputType, InputType
---

# funcac

Creates an activity function method

```

[FunctionName(nameof($ActivityClassName$))]
public async Task<$OutputType$> Run(
    [ActivityTrigger] $InputType$ input,
    ILogger logger)
{
    $END$
    // TODO
}
```
