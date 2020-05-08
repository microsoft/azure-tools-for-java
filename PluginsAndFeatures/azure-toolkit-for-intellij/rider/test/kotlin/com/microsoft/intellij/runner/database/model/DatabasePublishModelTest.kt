package com.microsoft.intellij.runner.database.model

import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import org.jetbrains.mock.SqlDatabaseMock
import org.testng.annotations.Test

class DatabasePublishModelTest {

    @Test
    fun testResetOnPublish_Defaults() {
        val mockDatabase = SqlDatabaseMock()

        val dbModel = DatabasePublishModel()
        dbModel.resetOnPublish(mockDatabase)

        dbModel.isDatabaseConnectionEnabled.shouldBeTrue()

        dbModel.isCreatingSqlDatabase.shouldBeFalse()
        dbModel.databaseId = mockDatabase.id()

        dbModel.isCreatingSqlServer.shouldBeFalse()
        dbModel.sqlServerName.shouldBeEmpty()

        dbModel.isCreatingResourceGroup.shouldBeFalse()
        dbModel.resourceGroupName.shouldBeEmpty()
    }
}
