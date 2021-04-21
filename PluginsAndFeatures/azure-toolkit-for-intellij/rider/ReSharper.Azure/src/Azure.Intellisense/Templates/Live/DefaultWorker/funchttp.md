---
guid: 2daa6cd4-0aef-4adb-852e-359dcb38593c
type: Live
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
scopes: InCSharpFile(minimumLanguageVersion=2.0)
parameterOrder: CLASS, AUTHLEVELVALUE
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
AUTHLEVELVALUE-expression: list("Function,Anonymous,Admin")
---

# funchttp

Creates an Azure Function method with an HTTP trigger.

```
[FunctionName("$CLASS$")]
public static async Task<IActionResult> RunAsync([HttpTrigger(AuthorizationLevel.$AUTHLEVELVALUE$, "get", "post", Route = null)]HttpRequest req, ILogger log)
{
    log.LogInformation("C# HTTP trigger function processed a request.");

    string name = req.Query["name"];

    string requestBody = await new StreamReader(req.Body).ReadToEndAsync();
    dynamic data = JsonConvert.DeserializeObject(requestBody);
    name = name ?? data?.name;

    return name != null
        ? (ActionResult)new OkObjectResult($"Hello, {name}")
        : new BadRequestObjectResult("Please pass a name on the query string or in the request body");$END$
}
```
