/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.helpers.validator

import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import org.jetbrains.mock.SqlServerMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class SqlServerValidatorTest {

    //region Sql Server

    @Test
    fun testCheckSqlServerIsSet_IsSet() {
        val validationResult = SqlServerValidator.checkSqlServerIsSet(SqlServerMock())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckSqlServerIsSet_IsNotSet() {
        val validationResult = SqlServerValidator.checkSqlServerIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.not_defined"))
    }

    //endregion Sql Server

    //region ID

    @Test
    fun testCheckSqlServerIdIsSet_IsSet() {
        val validationResult = SqlServerValidator.checkSqlServerIdIsSet("test-sql-server-id")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckSqlServerIdIsSet_IsNotSet() {
        val validationResult = SqlServerValidator.checkSqlServerIdIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.id_not_defined"))
    }

    @Test
    fun testCheckSqlServerIdIsSet_IsEmpty() {
        val validationResult = SqlServerValidator.checkSqlServerIdIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.id_not_defined"))
    }

    //endregion ID

    //region Name

    @Test
    fun testCheckSqlServerNameIsSet_IsSet() {
        val validationResult = SqlServerValidator.checkSqlServerNameIsSet("test-sql-server")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckSqlServerNameIsSet_IsEmpty() {
        val validationResult = SqlServerValidator.checkSqlServerNameIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.name_not_defined"))
    }

    //endregion Name

    //region Name Max Length

    @Test
    fun testCheckSqlServerNameMaxLength_BelowMax() {
        val validationResult = SqlServerValidator.checkSqlServerNameMaxLength("a".repeat(62))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckSqlServerNameMaxLength_Max() {
        val validationResult = SqlServerValidator.checkSqlServerNameMaxLength("a".repeat(63))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckSqlServerNameMaxLength_AboveMax() {
        val validationResult = SqlServerValidator.checkSqlServerNameMaxLength("a".repeat(64))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.name_length_error", 1, 63))
    }

    //endregion Name Max Length

    //region Name Min Length

    @Test
    fun testCheckSqlServerNameMinLength_BelowMin() {
        val validationResult = SqlServerValidator.checkSqlServerNameMinLength("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.name_length_error", 1, 63))
    }

    @Test
    fun testCheckSqlServerNameMinLength_Min() {
        val validationResult = SqlServerValidator.checkSqlServerNameMinLength("a")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckSqlServerNameMinLength_AboveMin() {
        val validationResult = SqlServerValidator.checkSqlServerNameMinLength("a".repeat(2))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Name Min Length

    //region Starts Ends With Dash

    @DataProvider(name = "checkStartsEndsWithDashData")
    fun checkStartsEndsWithDashData() = arrayOf(
            arrayOf("StartsWithDash", "-sql-server-name"),
            arrayOf("EndsWithDash", "sql-server-name-"),
            arrayOf("StartsEndsWithDashes", "-sql-server-name-")
    )

    @Test(dataProvider = "checkStartsEndsWithDashData")
    fun testCheckStartsEndsWithDash_StartsWithDash(name: String, sqlServerName: String) {
        val validationResult = SqlServerValidator.checkStartsEndsWithDash(sqlServerName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.name_cannot_start_end_with_dash"))
    }

    @Test
    fun testCheckStartsEndsWithDash_NotStartEndsWithDash() {
        val validationResult = SqlServerValidator.checkStartsEndsWithDash("test-sql-server")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Starts Ends With Dash

    //region Existence

    @Test
    fun testCheckSqlServerExistence_Exists() {
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                "test-subscription" to listOf(mockSqlServer)
        ))

        val validationResult = SqlServerValidator.checkSqlServerExistence(
                subscriptionId = "test-subscription", name = mockSqlServer.name())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.name_already_exists", mockSqlServer.name()))
    }

    @Test
    fun testCheckSqlServerExistence_NotExist() {
        val mockSqlServer = SqlServerMock(name = "test-sql-server")
        AzureSqlServerMvpModel.setSubscriptionIdToSqlServersMap(mapOf(
                "test-subscription" to listOf(mockSqlServer)
        ))

        val validationResult = SqlServerValidator.checkSqlServerExistence(
                subscriptionId = "test-subscription", name = "test-sql-server-1")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Existence

    //region Invalid Characters

    @DataProvider(name = "sqlServerNameInvalidCharactersData")
    fun sqlServerNameInvalidCharactersData() = arrayOf(
            arrayOf("CapitalizedChar", "SqlServerName", arrayOf('S', 'N')),
            arrayOf("Space", "sql server name", arrayOf(' ')),
            arrayOf("NonAlphabeticChar", "£∂®-sql-server-¢≥", arrayOf('£', '∂', '®', '¢', '≥')),
            arrayOf("Symbols", "sql/server&name", arrayOf('/', '&'))
    )

    @Test(dataProvider = "sqlServerNameInvalidCharactersData")
    fun testCheckInvalidCharacters_InvalidCharacters(name: String, sqlServerName: String, invalidChars: Array<Char>) {
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = SqlServerValidator.checkInvalidCharacters(sqlServerName)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.sql_server.name_invalid")} $invalidCharsString.")
    }

    @DataProvider(name = "sqlServerNameValidCharactersData")
    fun sqlServerNameValidCharactersData() = arrayOf(
            arrayOf("LowCaseLetters", "sqlserver"),
            arrayOf("Numbers", "sqlserver123"),
            arrayOf("Dash", "sql-server")
    )

    @Test(dataProvider = "sqlServerNameValidCharactersData")
    fun testCheckInvalidCharacters_ValidCharacters(name: String, sqlServerName: String) {
        val validationResult = SqlServerValidator.checkInvalidCharacters(sqlServerName)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Invalid Characters

    //region Validate Name

    @Test
    fun testValidateSqlServerName_IsNotSet() {
        val validationResult = SqlServerValidator.validateSqlServerName("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.name_not_defined"))
    }

    @Test
    fun testValidateSqlServerName_MultipleErrors() {
        val invalidChars = arrayOf('S', 'Q', 'L', '.')
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = SqlServerValidator.validateSqlServerName("test-SQL-server.name-".repeat(4))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(3)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.name_cannot_start_end_with_dash"))
        validationResult.errors[1].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.name_length_error", 1, 63))
        validationResult.errors[2].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.sql_server.name_invalid")} $invalidCharsString.")
    }

    @Test
    fun testValidateSqlServerName_Valid() {
        val validationResult = SqlServerValidator.validateSqlServerName("test-sql-name")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Validate Name

    //region Admin Login

    @Test
    fun testCheckAdminLoginIsSet_IsSet() {
        val validationResult = SqlServerValidator.checkAdminLoginIsSet("user")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckAdminLoginIsSet_IsNotSet() {
        val validationResult = SqlServerValidator.checkAdminLoginIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_not_defined"))
    }

    @Test
    fun testCheckAdminLoginIsSet_IsEmpty() {
        val validationResult = SqlServerValidator.checkAdminLoginIsSet("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_not_defined"))
    }

    //endregion Admin Login

    //region Admin Login Restricted

    @DataProvider(name = "restrictedAdminLoginsData")
    fun restrictedAdminLoginsData() = arrayOf(
            arrayOf("Admin", "admin"),
            arrayOf("Administrator", "administrator"),
            arrayOf("Sa", "sa"),
            arrayOf("Root", "root"),
            arrayOf("Dbmanager", "dbmanager"),
            arrayOf("Loginmanager", "loginmanager"),
            arrayOf("Dbo", "dbo"),
            arrayOf("Guest", "guest"),
            arrayOf("Public", "public")
    )

    @Test(dataProvider = "restrictedAdminLoginsData")
    fun testCheckRestrictedLogins_Restricted(name: String, adminLogin: String) {
        val validationResult = SqlServerValidator.checkRestrictedLogins(adminLogin)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_from_restriction_list",
                adminLogin, "'admin', 'administrator', 'sa', 'root', 'dbmanager', 'loginmanager', 'dbo', 'guest', 'public'"))
    }

    @Test
    fun testCheckRestrictedLogins_NotRestricted() {
        val validationResult = SqlServerValidator.checkRestrictedLogins("user")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Admin Login Restricted

    //region Admin Login Invalid Characters

    @Test
    fun testCheckLoginInvalidCharacters_InvalidCharacters_StartsWithDigit() {
        val validationResult = SqlServerValidator.checkLoginInvalidCharacters("0user")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_cannot_begin_with_digit_nonword"))
    }

    @Test
    fun testCheckLoginInvalidCharacters_InvalidCharacters_StartsWithSymbol() {
        val invalidChars = arrayOf('-')
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")
        val validationResult = SqlServerValidator.checkLoginInvalidCharacters("-user")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(2)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_cannot_begin_with_digit_nonword"))
        validationResult.errors[1].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_invalid")} $invalidCharsString.")
    }

    @DataProvider(name = "adminLoginInvalidCharactersData")
    fun adminLoginInvalidCharactersData() = arrayOf(
            arrayOf("NonAlphabeticChar", "sql£∂®server¢≥user", arrayOf('£', '∂', '®', '¢', '≥')),
            arrayOf("Symbols", "sql/server&user", arrayOf('/', '&')),
            arrayOf("Dash", "sql-server-user", arrayOf('-')),
            arrayOf("Space", "sql server user", arrayOf(' ')),
            arrayOf("Nbsp", "sql${'\u00A0'}server${'\u00A0'}user", arrayOf('\u00A0'))
    )

    @Test(dataProvider = "adminLoginInvalidCharactersData")
    fun testCheckLoginInvalidCharacters_InvalidCharacters(name: String, adminLogin: String, invalidChars: Array<Char>) {
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = SqlServerValidator.checkLoginInvalidCharacters(adminLogin)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_invalid")} $invalidCharsString.")
    }

    @Test
    fun testCheckLoginInvalidCharacters_MultipleErrors() {
        val invalidChars = arrayOf(' ', '&')
        val invalidCharsString = invalidChars.joinToString("', '", "'", "'")

        val validationResult = SqlServerValidator.checkLoginInvalidCharacters("0sql server&user")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(2)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_cannot_begin_with_digit_nonword"))
        validationResult.errors[1].shouldBe("${RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_invalid")} $invalidCharsString.")
    }

    @DataProvider(name = "adminLoginValidCharactersData")
    fun adminLoginValidCharactersData() = arrayOf(
            arrayOf("CapitalizedChar", "User"),
            arrayOf("LowCaseLetters", "user"),
            arrayOf("Numbers", "user123")
    )

    @Test(dataProvider = "adminLoginValidCharactersData")
    fun testCheckLoginInvalidCharacters_ValidCharacters(name: String, adminLogin: String) {
        val validationResult = SqlServerValidator.checkLoginInvalidCharacters(adminLogin)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Admin Login Invalid Characters

    //region Validate Admin Login

    @Test
    fun testValidateAdminLogin_IsNotSet() {
        val validationResult = SqlServerValidator.validateAdminLogin("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_not_defined"))
    }

    @Test
    fun testValidateAdminLogin_RestrictedLogin() {
        val validationResult = SqlServerValidator.validateAdminLogin("admin")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_login_from_restriction_list",
                "admin", "'admin', 'administrator', 'sa', 'root', 'dbmanager', 'loginmanager', 'dbo', 'guest', 'public'"))
    }

    @Test
    fun testValidateAdminLogin_IsSet() {
        val validationResult = SqlServerValidator.validateAdminLogin("user")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Validate Admin Login

    //region Admin Password

    @Test
    fun testCheckPasswordIsSet_IsSet() {
        val validationResult = SqlServerValidator.checkPasswordIsSet("password".toCharArray())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckPasswordIsSet_IsNotSet() {
        val validationResult = SqlServerValidator.checkPasswordIsSet(CharArray(0))

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_not_defined"))
    }

    //endregion Admin Password

    //region Admin Password Max Length

    @Test
    fun testCheckPasswordMaxLength_BelowMax() {
        val validationResult = SqlServerValidator.checkPasswordMaxLength("a".repeat(127).toCharArray())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckPasswordMaxLength_Max() {
        val validationResult = SqlServerValidator.checkPasswordMaxLength("a".repeat(128).toCharArray())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckPasswordMaxLength_AboveMax() {
        val validationResult = SqlServerValidator.checkPasswordMaxLength("a".repeat(129).toCharArray())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_length_error", 8, 128))
    }

    //endregion Admin Password Max Length

    //region Admin Password Min Length

    @Test
    fun testCheckPasswordMinLength_BelowMin() {
        val validationResult = SqlServerValidator.checkPasswordMinLength("a".repeat(7).toCharArray())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_length_error", 8, 128))
    }

    @Test
    fun testCheckPasswordMinLength_Min() {
        val validationResult = SqlServerValidator.checkPasswordMinLength("a".repeat(8).toCharArray())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckPasswordMinLength_AboveMin() {
        val validationResult = SqlServerValidator.checkPasswordMinLength("a".repeat(9).toCharArray())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Admin Password Min Length

    //region Admin Password Contains User Name

    @Test
    fun testCheckPasswordContainsUsername_Contains() {
        val validationResult = SqlServerValidator.checkPasswordContainsUsername(
                password = "userpassword".toCharArray(), username = "user")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_cannot_contain_part_of_login"))
    }

    @Test
    fun testCheckPasswordContainsUsername_NotContain() {
        val validationResult = SqlServerValidator.checkPasswordContainsUsername(
                password = "password".toCharArray(), username = "user")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Admin Password Contains User Name

    //region Admin Password Requirements

    @DataProvider(name = "notPassPasswordRequirementsData")
    fun notPassPasswordRequirementsData() = arrayOf(
            arrayOf("NoPass", " ".toCharArray()),
            arrayOf("LowerCase", "password".toCharArray()),
            arrayOf("DifferentCase", "Password".toCharArray())
    )

    @Test(dataProvider = "notPassPasswordRequirementsData")
    fun testCheckPasswordRequirements_NotPass(name: String, password: CharArray) {
        val validationResult = SqlServerValidator.checkPasswordRequirements(password)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_category_check_failed"))
    }

    @DataProvider(name = "passPasswordRequirementsData")
    fun passPasswordRequirementsData() = arrayOf(
            arrayOf("DifferentCaseWithDigits", "Passw0rd".toCharArray()),
            arrayOf("DigitsWithNonAlphabetical", "pa\$\$w0rd".toCharArray()),
            arrayOf("AllPass", "Pa\$sw0rd".toCharArray())
    )

    @Test(dataProvider = "passPasswordRequirementsData")
    fun testCheckPasswordRequirements_Pass(name: String, password: CharArray) {
        val validationResult = SqlServerValidator.checkPasswordRequirements(password)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Admin Password Requirements

    //region Admin Password Match

    @Test
    fun testCheckPasswordsMatch_ExactMatch() {
        val validationResult = SqlServerValidator.checkPasswordsMatch("password".toCharArray(), "password".toCharArray())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckPasswordsMatch_DifferentCase() {
        val validationResult = SqlServerValidator.checkPasswordsMatch("password".toCharArray(), "Password".toCharArray())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_do_not_match"))
    }

    @Test
    fun testCheckPasswordsMatch_NotMatch() {
        val validationResult = SqlServerValidator.checkPasswordsMatch("password".toCharArray(), "otherpass".toCharArray())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_do_not_match"))
    }

    //endregion Admin Password Match

    //region Validate Admin Password

    @Test
    fun testValidateAdminPassword_IsNotSet() {
        val validationResult = SqlServerValidator.validateAdminPassword(
                username = "user", password = "".toCharArray())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_not_defined"))
    }

    @Test
    fun testValidateAdminPassword_ContainsAdminLogin() {
        val validationResult = SqlServerValidator.validateAdminPassword(
                username = "user", password = "userpassword".toCharArray())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_cannot_contain_part_of_login"))
    }

    @Test
    fun testValidateAdminPassword_MultipleErrors() {
        val validationResult = SqlServerValidator.validateAdminPassword(
                username = "user", password = "pass".toCharArray())

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(2)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_length_error", 8, 128))
        validationResult.errors[1].shouldBe(RiderAzureBundle.message("run_config.publish.validation.sql_server.admin_password_category_check_failed"))
    }

    @Test
    fun testValidateAdminPassword_Valid() {
        val validationResult = SqlServerValidator.validateAdminPassword("user", "Pa#sw0rd".toCharArray())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Validate Admin Password
}
