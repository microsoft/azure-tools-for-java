using System.Threading.Tasks;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Extensions.Logging;

[assembly: Function("HttpFunction1")]
namespace FunctionAppIsolated
{
    public class Function
    {
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