### Configuring MS Application Insights

#### Prerequisites
- An Azure account with an active subscription.

#### 1. Generate the Instrumentation Key

1. **Log in to the Azure Portal**: Go to the [Azure Portal](https://portal.azure.com/).

2. **Create a new Application Insights resource**:
    - In the Azure Portal, click on `Create a resource`.
    - Search for `Application Insights` and select it.
    - Click `Create`.
    - Fill in the required fields such as `Name`, `Subscription`, `Resource Group`, and `Region`.
    - Click `Review + create`, then click `Create`.

3. **Get the Instrumentation Key**:
    - After the resource is created, navigate to the Application Insights resource.
    - In the left-hand menu, under `Configure`, click on `Properties`.
    - Copy the `Instrumentation Key`.

#### 2. Add the Instrumentation Key to the Application

1. **Create `applicationinsights.json`**:
    - Navigate to the `resources/META-INF` directory of your project. If the directory doesn't exist, create it.
    - Create a file named `applicationinsights.json` in the `resources/META-INF` directory.

2. **Add the Instrumentation Key to `applicationinsights.json`**:
    - Open the `applicationinsights.json` file and add the following JSON content:
   ```json
   {
     "instrumentationKey": "your-instrumentation-key-goes-here"
   }
   ```
    - Replace `"your-instrumentation-key-goes-here"` with the actual instrumentation key you copied from the Azure Portal.

#### 3. Add `applicationinsights.json` to `.gitignore`

To prevent exposing your instrumentation key in version control, add the `applicationinsights.json` file to your `.gitignore` file.

1. **Open `.gitignore`**:
    - If you don't have a `.gitignore` file in your project root, create one.

2. **Add `applicationinsights.json` to `.gitignore`**:
    - Add the following line to the `.gitignore` file:
   ```
   resources/META-INF/applicationinsights.json
   ```

#### 4. App Insights Configuration in Code
1. **Accurate Tracking of Method Calls**:
   - The `visitMethodCallExpression` method tracks the number of times each method of each client is called.
   - The `methodCounts` map stores counts in a nested map structure, where the outer map's key is the client name, and the value is another map. This inner map's key is the method name, and the value is the count of calls to that method.

2. **Periodic Reporting of Data**:
   - The `startTelemetryService` method schedules the `sendTelemetryData` method to run at fixed intervals (every 3 minutes, starting 2 minutes after the service starts).
   - Each execution of `sendTelemetryData` will report the accumulated counts since the last report.

3. **Structured Telemetry Events**:
   - Each method call count is reported as an event with the name `azure_sdk_usage_frequency`.
   - Custom dimensions are added to these events, including:
      - `clientName`: The name of the client (e.g., `BlobClient`).
      - `methodName`: The name of the method (e.g., `download`).
      - `count`: The count of calls to the method (e.g., `5`), stored as a string.

### Sample Output

The telemetry system will generate events that can be queried and analyzed. Here is an example of what the telemetry data might look like:

#### Event 1
- **Name**: `azure_sdk_usage_frequency`
- **Custom Dimensions**:
   - `clientName`: `BlobClient`
   - `methodName`: `download`
   - `count`: `5`

#### Event 2
- **Name**: `azure_sdk_usage_frequency`
- **Custom Dimensions**:
   - `clientName`: `BlobClient`
   - `methodName`: `upload`
   - `count`: `3`