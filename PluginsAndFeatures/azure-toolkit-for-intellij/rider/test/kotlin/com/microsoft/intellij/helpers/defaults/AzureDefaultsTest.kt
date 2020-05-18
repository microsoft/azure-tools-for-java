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

package com.microsoft.intellij.helpers.defaults

import com.jetbrains.rider.test.asserts.shouldBe
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.DatabaseEdition
import org.testng.annotations.Test

class AzureDefaultsTest {

    @Test
    fun testSqlDatabaseCollation_DefaultValue() {
        AzureDefaults.SQL_DATABASE_COLLATION.shouldBe("SQL_Latin1_General_CP1_CI_AS")
    }

    @Test
    fun testSqlDatabaseEdition_DefaultValue() {
        AzureDefaults.databaseEdition.shouldBe(DatabaseEdition.BASIC)
    }

    @Test
    fun testLocation_DefaultValue() {
        AzureDefaults.location.shouldBe(Region.US_EAST)
    }

    @Test
    fun testPricingTier_DefaultValue() {
        AzureDefaults.pricingTier.shouldBe(PricingTier.STANDARD_S1)
    }
}
