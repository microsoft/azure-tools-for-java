package com.microsoft.intellij.runner.webapp.webappconfig

class UiConstants {
    companion object {
        const val APP_SERVICE_PLAN_ALREADY_EXISTS = "App Service Plan with name '%s' already exists"
        const val APP_SERVICE_PLAN_ID_NOT_DEFINED = "App Service Plan ID is not defined"
        const val APP_SERVICE_PLAN_LOCATION_NOT_DEFINED = "App Service Plan Location is not defined"
        const val APP_SERVICE_PLAN_NAME_INVALID = "App Service Plan name cannot contain characters: %s"
        const val APP_SERVICE_PLAN_NAME_NOT_DEFINED = "App Service Plan name not provided"

        const val CONNECTION_STRING_CREATE_FAILED = "Failed to create Connection String to web app: %s"
        const val CONNECTION_STRING_CREATING = "Creating connection string with name '%s'..."
        const val CONNECTION_STRING_NAME_ALREADY_EXISTS = "Connection String with name '%s' already exists"
        const val CONNECTION_STRING_NAME_NOT_DEFINED = "Connection string not set"

        const val DEPLOY_SUCCESSFUL = "Deploy succeeded."

        const val LOCATION_NOT_DEFINED = "Location not provided"

        const val PROJECT_ARTIFACTS_COLLECTING = "Collecting '%s' project artifacts..."
        const val PROJECT_ARTIFACTS_COLLECTING_FAILED = "Failed collecting project artifacts. Please see Build output"

        const val PROJECT_NOT_DEFINED = "Project is not defined"

        const val PUBLISH_DONE = "Done."

        const val RESOURCE_GROUP_ALREADY_EXISTS = "Resource Group with name '%s' already exists"
        const val RESOURCE_GROUP_NAME_CANNOT_ENDS_WITH_PERIOD = "Resource Group name cannot ends with period symbol"
        const val RESOURCE_GROUP_NAME_INVALID = "Resource Group name cannot contain characters: %s"
        const val RESOURCE_GROUP_NAME_NOT_DEFINED = "Resource Group name not provided"

        const val SIGN_IN_REQUIRED = "Please sign in with your Azure account"

        const val SQL_DATABASE_COLLATION_NOT_DEFINED = "SQL Database Collation not provided"
        const val SQL_DATABASE_CREATE = "Creating SQL Database '%s'..."
        const val SQL_DATABASE_CREATE_SUCCESSFUL = "SQL Database is created successfully."
        const val SQL_DATABASE_GET_EXISTING = "Got existing SQL Database: %s"
        const val SQL_DATABASE_NAME_ALREADY_EXISTS = "SQL Database name '%s' already exists"
        const val SQL_DATABASE_NAME_INVALID = "SQL Database name cannot contain characters: %s"
        const val SQL_DATABASE_NAME_NOT_DEFINED = "SQL Database Name is not defined"
        const val SQL_DATABASE_NOT_DEFINED = "Database not set"
        const val SQL_DATABASE_URL = "Please see SQL Database details by URL: %s"

        const val SQL_SERVER_ADMIN_LOGIN_CANNOT_BEGIN_WITH_DIGIT_NONWORD = "SQL Server Admin login must not begin with numbers or symbols"
        const val SQL_SERVER_ADMIN_LOGIN_CANNOT_CONTAIN_WHITESPACES = "SQL Server Admin login cannot contain whitespaces"
        const val SQL_SERVER_ADMIN_LOGIN_FROM_RESTRICTED_LIST = "SQL Server Admin login '%s' is from list of restricted SQL Admin names: %s"
        const val SQL_SERVER_ADMIN_LOGIN_INVALID = "SQL Server Admin login should not unicode characters, or nonalphabetic characters."
        const val SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED = "SQL Server Admin Login is not defined"
        const val SQL_SERVER_ADMIN_PASSWORD_CANNOT_CONTAIN_PART_OF_LOGIN =
                "Your password cannot contain all or part of the login name. Part of a login name is defined as three or more consecutive alphanumeric characters."
        const val SQL_SERVER_ADMIN_PASSWORD_CATEGORY_CHECK_FAILED =
                "Your password must contain characters from three of the following categories – English uppercase letters, " +
                "English lowercase letters, numbers (0-9), and non-alphanumeric characters (!, \$, #, %, etc.)."
        const val SQL_SERVER_ADMIN_PASSWORD_DOES_NOT_MATCH = "Passwords do not match"
        const val SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED = "SQL Server Admin Password is not defined"
        const val SQL_SERVER_CANNOT_GET = "Unable to find SQL Server with name '%s'"
        const val SQL_SERVER_CREATE = "Creating SQL Server '%s'..."
        const val SQL_SERVER_CREATE_SUCCESSFUL = "SQL Server is created, id: '%s'"
        const val SQL_SERVER_GET_EXISTING = "Get existing SQL Server with Id: '%s'"
        const val SQL_SERVER_ID_NOT_DEFINED = "SQL Server ID is not defined"
        const val SQL_SERVER_NAME_ALREADY_EXISTS = "SQL Server name '%s' already exists"
        const val SQL_SERVER_NAME_CANNOT_START_END_WITH_DASH = "SQL Server name cannot begin or end with '-' symbol"
        const val SQL_SERVER_NAME_INVALID = "SQL Server name cannot contain characters: %s"
        const val SQL_SERVER_NAME_NOT_DEFINED = "SQL Server Name is not defined"
        const val SQL_SERVER_REGION_NOT_DEFINED = "SQL Server Region is not defined"
        const val SQL_SERVER_RESOURCE_GROUP_NAME_NOT_DEFINED = "SQL Server Resource Group Name is not defined"

        const val SUBSCRIPTION_NOT_DEFINED = "Subscription not provided"
        const val SUBSCRIPTION_DISABLED = "Subscription '%s' is disabled"
        const val SUBSCRIPTION_DELETED = "Subscription '%s' is deleted"

        const val WEB_APP_ALREADY_EXISTS = "Web App with name '%s' already exists"
        const val WEB_APP_CREATE = "Creating Web App '%s'..."
        const val WEB_APP_CREATE_SUCCESSFUL = "Web App  is created '%s'"
        const val WEB_APP_GET_EXISTING = "Got existing Web App with Id: '%s'"
        const val WEB_APP_NOT_DEFINED = "Please select an Azure Web App"
        const val WEB_APP_ID_NOT_DEFINED = "Web App ID is not defined"
        const val WEB_APP_NAME_CANNOT_START_END_WITH_DASH = "Web App name cannot begin or end with '-' symbol"
        const val WEB_APP_NAME_INVALID = "Web App name cannot contain characters: %s"
        const val WEB_APP_NAME_NOT_DEFINED = "Web App name not provided"
        const val WEB_APP_SET_STARTUP_FILE = "Set Startup File for a web app '%s' to: '%s'"
        const val WEB_APP_SETTING_DOCKER_CUSTOM_IMAGE_NAME = "DOCKER_CUSTOM_IMAGE_NAME"
        const val WEB_APP_START = "Start Web App '%s'..."
        const val WEB_APP_STARTUP_COMMAND_TEMPLATE = "dotnet %s"
        const val WEB_APP_STOP = "Stop Web App '%s'..."
        const val WEB_APP_TARGET_NAME = "Microsoft.WebApplication.targets"

        const val ZIP_DEPLOY_PUBLISH_FAIL = "Fail publishing ZIP file"
        const val ZIP_DEPLOY_PUBLISH_SUCCESS = "Published ZIP file successfully"
        const val ZIP_DEPLOY_START_PUBLISHING = "Publishing ZIP file. Attempt %s of %s..."

        const val ZIP_FILE_CREATE_FOR_PROJECT = "Creating '%s' project ZIP..."
        const val ZIP_FILE_CREATE_SUCCESSFUL = "Project ZIP is created: '%s'"
        const val ZIP_FILE_DELETING = "Deleting ZIP file '%s'"
        const val ZIP_FILE_NOT_CREATED = "Unable to create a ZIP file"
    }
}