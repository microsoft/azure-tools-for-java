/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers

class UiConstants {
    companion object {
        const val APP_SERVICE_PLAN_ID_NOT_DEFINED = "App Service Plan ID is not defined"
        const val APP_SERVICE_PLAN_LOCATION_NOT_DEFINED = "App Service Plan Location is not defined"
        const val APP_SERVICE_PLAN_NAME_NOT_DEFINED = "App Service Plan name not provided"

        const val CONNECTION_STRING_CREATE_FAILED = "Failed to create Connection String to web app: %s"
        const val CONNECTION_STRING_CREATING = "Creating connection string with name '%s'..."
        const val CONNECTION_STRING_NAME_NOT_DEFINED = "Connection string not set"

        const val DEPLOY_SUCCESSFUL = "Deploy succeeded."

        const val PROJECT_ARTIFACTS_COLLECTING = "Collecting '%s' project artifacts..."
        const val PROJECT_ARTIFACTS_COLLECTING_FAILED = "Failed collecting project artifacts. Please see Build output"

        const val PROJECT_NOT_DEFINED = "Project is not defined"

        const val PUBLISH_DONE = "Done."

        const val RESOURCE_GROUP_NAME_NOT_DEFINED = "Resource Group name not provided"

        const val SIGN_IN_REQUIRED = "Please sign in with your Azure account"

        const val SQL_DATABASE_CREATE = "Creating SQL Database '%s'..."
        const val SQL_DATABASE_CREATE_SUCCESSFUL = "SQL Database is created successfully."
        const val SQL_DATABASE_GET_EXISTING = "Got existing SQL Database: %s"
        const val SQL_DATABASE_NAME_NOT_DEFINED = "SQL Database Name is not defined"
        const val SQL_DATABASE_NOT_DEFINED = "Database not set"
        const val SQL_DATABASE_URL = "Please see SQL Database details by URL: %s"

        const val SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED = "SQL Server Admin Login is not defined"
        const val SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED = "SQL Server Admin Password is not defined"
        const val SQL_SERVER_CANNOT_GET = "Unable to find SQL Server with name '%s'"
        const val SQL_SERVER_CREATE = "Creating SQL Server '%s'..."
        const val SQL_SERVER_CREATE_SUCCESSFUL = "SQL Server is created, id: '%s'"
        const val SQL_SERVER_GET_EXISTING = "Get existing SQL Server with Id: '%s'"
        const val SQL_SERVER_ID_NOT_DEFINED = "SQL Server ID is not defined"
        const val SQL_SERVER_NAME_NOT_DEFINED = "SQL Server Name is not defined"
        const val SQL_SERVER_REGION_NOT_DEFINED = "SQL Server Region is not defined"
        const val SQL_SERVER_RESOURCE_GROUP_NAME_NOT_DEFINED = "SQL Server Resource Group Name is not defined"

        const val SUBSCRIPTION_NOT_DEFINED = "Subscription not provided"

        const val WEB_APP_CREATE = "Creating Web App '%s'..."
        const val WEB_APP_CREATE_SUCCESSFUL = "Web App  is created '%s'"
        const val WEB_APP_GET_EXISTING = "Got existing Web App with Id: '%s'"
        const val WEB_APP_ID_NOT_DEFINED = "Web App ID is not defined"
        const val WEB_APP_NAME_NOT_DEFINED = "Web App name not provided"
        const val WEB_APP_SET_STARTUP_FILE = "Set Startup File for a web app '%s' to: '%s'"
        const val WEB_APP_SETTING_DOCKER_CUSTOM_IMAGE_NAME = "DOCKER_CUSTOM_IMAGE_NAME"
        const val WEB_APP_START = "Start Web App '%s'..."
        const val WEB_APP_STARTUP_COMMAND_TEMPLATE = "dotnet %s"
        const val WEB_APP_STOP = "Stop Web App '%s'..."

        const val FUNCTION_APP_CREATE = "Creating Function App '%s'..."
        const val FUNCTION_APP_CREATE_SUCCESSFUL = "Function App  is created '%s'"
        const val FUNCTION_APP_GET_EXISTING = "Got existing Function App with Id: '%s'"
        const val FUNCTION_APP_ID_NOT_DEFINED = "Function App ID is not defined"
        const val FUNCTION_APP_NAME_NOT_DEFINED = "Function App name not provided"
        const val FUNCTION_APP_SET_STARTUP_FILE = "Set Startup File for a function app '%s' to: '%s'"
        const val FUNCTION_APP_SETTING_DOCKER_CUSTOM_IMAGE_NAME = "DOCKER_CUSTOM_IMAGE_NAME"
        const val FUNCTION_APP_START = "Start Function App '%s'..."
        const val FUNCTION_APP_STARTUP_COMMAND_TEMPLATE = "dotnet %s"
        const val FUNCTION_APP_STOP = "Stop Function App '%s'..."

        const val ZIP_DEPLOY_PUBLISH_FAIL = "Fail publishing ZIP file"
        const val ZIP_DEPLOY_PUBLISH_SUCCESS = "Published ZIP file successfully"
        const val ZIP_DEPLOY_START_PUBLISHING = "Publishing ZIP file. Attempt %s of %s..."

        const val ZIP_FILE_CREATE_FOR_PROJECT = "Creating '%s' project ZIP..."
        const val ZIP_FILE_CREATE_SUCCESSFUL = "Project ZIP is created: '%s'"
        const val ZIP_FILE_DELETING = "Deleting ZIP file '%s'"
        const val ZIP_FILE_NOT_CREATED = "Unable to create a ZIP file"
    }
}
