using System;
using System.Threading.Tasks;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace FunctionAppIsolated
{
    public static class Function
    {
        [Function("TimerTrigger")]
        public static async Task RunAsync([TimerTrigger(<caret>)] TimerInfo myTimer, ILogger log)
        {
            log.LogInformation($"C# Timer trigger function executed at: {DateTime.UtcNow}");
            
        }
    }
}