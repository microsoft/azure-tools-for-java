using System.Threading.Tasks;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Extensions.Logging;

namespace FunctionAppIsolated
{
    public class HttpFunction
    {
        [Function(nameof(HttpFunction1))]
        public async Task<string> HttpFunction1(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post", Route = "test")] HttpRequestData req,
            FunctionContext functionContext)
        {
            var log = functionContext.GetLogger<HttpFunction>();
            log.LogInformation("You called the trigger!");

            return "Hello world";
        }

        [Function("RunTimerTrigger")]
        public static async Task RunTimerTrigger([TimerTrigger("0 */5 * * * *")] TimerInfo myTimer, ILogger log)
        {
            log.LogInformation($"C# Timer trigger function executed at: {DateTime.UtcNow}");

        }
    }
}