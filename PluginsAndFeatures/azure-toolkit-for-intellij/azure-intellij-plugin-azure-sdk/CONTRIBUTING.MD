### Configuring MS Application Insights
- An Azure account with an active subscription.

#### 1. Generate the Instrumentation Key

#### 2. Add the Instrumentation Key to the Application

Add it to the `applicationinsights.json` file located in the `resources/META-INF` directory of the `azure-intellij-plugin-azure-sdk` project.
```json
{
  "instrumentationKey": "your-instrumentation-key-goes-here"
}
```

#### 3. Add `applicationinsights.json` to `.gitignore`