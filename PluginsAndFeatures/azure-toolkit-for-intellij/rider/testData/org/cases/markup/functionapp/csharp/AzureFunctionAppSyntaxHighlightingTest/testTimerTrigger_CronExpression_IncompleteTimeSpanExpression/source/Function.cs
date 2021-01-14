using System;
using System.Threading.Tasks;
using Microsoft.Azure.WebJobs;
using Microsoft.Extensions.Logging;

namespace Company.FunctionApp
{
    public static class MyTimerTrigger
    {
        [FunctionName("MyTimerTrigger")]
        public static async Task RunAsync([TimerTrigger("00:12:")] TimerInfo myTimer, ILogger log)
        {
            log.LogInformation($"C# Timer trigger function executed at: {DateTime.UtcNow}");
        }
    }
}
