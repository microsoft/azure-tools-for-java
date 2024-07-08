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