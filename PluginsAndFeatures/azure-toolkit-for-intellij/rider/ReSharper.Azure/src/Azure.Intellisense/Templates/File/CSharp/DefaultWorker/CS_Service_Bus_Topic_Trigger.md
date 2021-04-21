---
guid: 5e6a4a74-7465-4e18-b1eb-a82294ad3391
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=ServiceBusTopicTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsDefaultWorker
parameterOrder: (HEADER), (NAMESPACE), (CLASS), TOPICNAME, SUBSCRIPTIONNAME, (CONNECTIONVALUE)
HEADER-expression: fileheader()
NAMESPACE-expression: fileDefaultNamespace()
CLASS-expression: getAlphaNumericFileNameWithoutExtension()
TOPICNAME-expression: constant("mytopic")
SUBSCRIPTIONNAME-expression: constant("mysubscription")
CONNECTIONVALUE-expression: constant("")
---

# Service Bus Topic Trigger

```
$HEADER$using System;
using System.Threading.Tasks;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [FunctionName("$CLASS$")]
        public static async Task RunAsync([ServiceBusTrigger("$TOPICNAME$", "$SUBSCRIPTIONNAME$", Connection = "$CONNECTIONVALUE$")]string mySbMsg, ILogger log)
        {
            log.LogInformation($"C# ServiceBus topic trigger function processed message: {mySbMsg}");$END$
        }
    }
}
```
