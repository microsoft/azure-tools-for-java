/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.intellij.helpers.validator.ResourceGroupValidator
import com.microsoft.intellij.helpers.validator.SqlDatabaseValidator
import com.microsoft.intellij.helpers.validator.SqlServerValidator
import com.microsoft.intellij.helpers.validator.SubscriptionValidator
import com.microsoft.intellij.runner.database.model.DatabasePublishModel

object SqlDatabaseConfigValidator : ConfigurationValidator() {

    @Throws(RuntimeConfigurationError::class)
    fun validateDatabaseConnection(model: DatabasePublishModel) {
        if (!model.isDatabaseConnectionEnabled) return

        if (model.isCreatingSqlDatabase) {
            checkStatus(SubscriptionValidator.validateSubscription(model.subscription))

            val subscriptionId = model.subscription!!.subscriptionId()

            checkStatus(SqlDatabaseValidator.validateDatabaseName(model.databaseName))
            checkStatus(SqlDatabaseValidator.checkSqlDatabaseExists(subscriptionId, model.databaseName, model.sqlServerName))

            if (model.isCreatingResourceGroup) {
                checkStatus(ResourceGroupValidator.validateResourceGroupName(model.resourceGroupName))
                checkStatus(ResourceGroupValidator.checkResourceGroupNameExists(subscriptionId, model.resourceGroupName))
            } else {
                checkStatus(ResourceGroupValidator.checkResourceGroupNameIsSet(model.resourceGroupName))
            }

            if (model.isCreatingSqlServer) {
                checkStatus(SqlServerValidator.validateSqlServerName(model.sqlServerName))
                checkStatus(SqlServerValidator.checkSqlServerExistence(subscriptionId, model.sqlServerName))
                checkStatus(SqlServerValidator.validateAdminLogin(model.sqlServerAdminLogin))
                checkStatus(SqlServerValidator.validateAdminPassword(model.sqlServerAdminLogin, model.sqlServerAdminPassword))
                checkStatus(SqlServerValidator.checkPasswordsMatch(model.sqlServerAdminPassword, model.sqlServerAdminPasswordConfirm))
            } else {
                checkStatus(SqlServerValidator.checkSqlServerIdIsSet(model.sqlServerId))
                checkStatus(SqlServerValidator.checkPasswordIsSet(model.sqlServerAdminPassword))
            }

            checkStatus(SqlDatabaseValidator.checkCollationIsSet(model.collation))
        } else {
            checkStatus(SqlDatabaseValidator.checkDatabaseIdIsSet(model.databaseId))
            checkStatus(SqlServerValidator.checkAdminLoginIsSet(model.sqlServerAdminLogin))
            checkStatus(SqlServerValidator.checkPasswordIsSet(model.sqlServerAdminPassword))
        }
    }
}
