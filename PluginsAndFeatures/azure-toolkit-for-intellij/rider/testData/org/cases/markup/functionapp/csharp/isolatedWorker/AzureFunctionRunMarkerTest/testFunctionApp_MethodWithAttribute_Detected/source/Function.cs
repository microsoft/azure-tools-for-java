using System.Threading.Tasks;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Http;
using Microsoft.Extensions.Logging;

namespace FunctionAppIsolated
{
    public static class Function
    {
        [Function("MainFunction")]
        public static void Main(string[] args)
        {
            Console.WriteLine("Hello World!");
        }
    }
}