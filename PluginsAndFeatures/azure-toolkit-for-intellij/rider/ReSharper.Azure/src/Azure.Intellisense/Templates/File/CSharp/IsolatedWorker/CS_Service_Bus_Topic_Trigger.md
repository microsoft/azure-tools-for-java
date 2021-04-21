---
guid: 7f50ad96-6a80-4be0-96b8-9d224997a9aa
type: File
reformat: True
shortenReferences: True
image: AzureFunctionsTrigger
customProperties: Extension=cs, FileName=ServiceBusTopicTrigger, ValidateFileName=True
scopes: InAzureFunctionsCSharpProject;MustUseAzureFunctionsIsolatedWorker
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
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace $NAMESPACE$
{
    public static class $CLASS$
    {
        [Function("$CLASS$")]
        public static void Run([ServiceBusTrigger("$TOPICNAME$", "$SUBSCRIPTIONNAME$", Connection = "$CONNECTIONVALUE$")] string mySbMsg, FunctionContext context)
        {
            var logger = context.GetLogger("$CLASS$");
            logger.LogInformation($"C# ServiceBus topic trigger function processed message: {mySbMsg}");$END$
        }
    }
}

```
