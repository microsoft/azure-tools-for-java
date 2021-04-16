using System.ComponentModel;
using System.Threading.Tasks;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Extensions.Logging;

namespace FunctionAppIsolated
{
    public class Function
    {
        [Function(nameof(HttpFunction1))]
        [DisplayName(nameof(HttpFunction1))]
        public async Task<string> HttpFunction1(
            [HttpTrigger(AuthorizationLevel.Anonymous, "get", "post", Route = "test")] HttpRequestData req,
            FunctionContext functionContext)
        {
            var log = functionContext.GetLogger<HttpFunction>();
            log.LogInformation("You called the trigger!");

            return "Hello world";
        }
    }
}