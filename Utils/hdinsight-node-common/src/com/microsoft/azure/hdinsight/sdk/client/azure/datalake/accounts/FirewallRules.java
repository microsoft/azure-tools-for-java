/**
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.models.CreateOrUpdateFirewallRuleParameters;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.models.FirewallRule;
import com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts.models.UpdateFirewallRuleParameters;
import com.microsoft.azure.ListOperationCallback;
import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import com.microsoft.rest.ServiceResponse;
import java.io.IOException;
import java.util.List;
import rx.Observable;

/**
 * An instance of this class provides access to all the operations defined
 * in FirewallRules.
 */
public interface FirewallRules {
    /**
     * Lists the Data Lake Analytics firewall rules within the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the PagedList&lt;FirewallRule&gt; object if successful.
     */
    PagedList<FirewallRule> listByAccount(final String resourceGroupName, final String accountName);

    /**
     * Lists the Data Lake Analytics firewall rules within the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<List<FirewallRule>> listByAccountAsync(final String resourceGroupName, final String accountName, final ListOperationCallback<FirewallRule> serviceCallback);

    /**
     * Lists the Data Lake Analytics firewall rules within the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the PagedList&lt;FirewallRule&gt; object
     */
    Observable<Page<FirewallRule>> listByAccountAsync(final String resourceGroupName, final String accountName);

    /**
     * Lists the Data Lake Analytics firewall rules within the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the PagedList&lt;FirewallRule&gt; object
     */
    Observable<ServiceResponse<Page<FirewallRule>>> listByAccountWithServiceResponseAsync(final String resourceGroupName, final String accountName);

    /**
     * Creates or updates the specified firewall rule. During update, the firewall rule with the specified name will be replaced with this new firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to create or update.
     * @param parameters Parameters supplied to create or update the firewall rule.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the FirewallRule object if successful.
     */
    FirewallRule createOrUpdate(String resourceGroupName, String accountName, String firewallRuleName, CreateOrUpdateFirewallRuleParameters parameters);

    /**
     * Creates or updates the specified firewall rule. During update, the firewall rule with the specified name will be replaced with this new firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to create or update.
     * @param parameters Parameters supplied to create or update the firewall rule.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<FirewallRule> createOrUpdateAsync(String resourceGroupName, String accountName, String firewallRuleName, CreateOrUpdateFirewallRuleParameters parameters, final ServiceCallback<FirewallRule> serviceCallback);

    /**
     * Creates or updates the specified firewall rule. During update, the firewall rule with the specified name will be replaced with this new firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to create or update.
     * @param parameters Parameters supplied to create or update the firewall rule.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the FirewallRule object
     */
    Observable<FirewallRule> createOrUpdateAsync(String resourceGroupName, String accountName, String firewallRuleName, CreateOrUpdateFirewallRuleParameters parameters);

    /**
     * Creates or updates the specified firewall rule. During update, the firewall rule with the specified name will be replaced with this new firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to create or update.
     * @param parameters Parameters supplied to create or update the firewall rule.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the FirewallRule object
     */
    Observable<ServiceResponse<FirewallRule>> createOrUpdateWithServiceResponseAsync(String resourceGroupName, String accountName, String firewallRuleName, CreateOrUpdateFirewallRuleParameters parameters);

    /**
     * Gets the specified Data Lake Analytics firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to retrieve.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the FirewallRule object if successful.
     */
    FirewallRule get(String resourceGroupName, String accountName, String firewallRuleName);

    /**
     * Gets the specified Data Lake Analytics firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to retrieve.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<FirewallRule> getAsync(String resourceGroupName, String accountName, String firewallRuleName, final ServiceCallback<FirewallRule> serviceCallback);

    /**
     * Gets the specified Data Lake Analytics firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to retrieve.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the FirewallRule object
     */
    Observable<FirewallRule> getAsync(String resourceGroupName, String accountName, String firewallRuleName);

    /**
     * Gets the specified Data Lake Analytics firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to retrieve.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the FirewallRule object
     */
    Observable<ServiceResponse<FirewallRule>> getWithServiceResponseAsync(String resourceGroupName, String accountName, String firewallRuleName);

    /**
     * Updates the specified firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to update.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the FirewallRule object if successful.
     */
    FirewallRule update(String resourceGroupName, String accountName, String firewallRuleName);

    /**
     * Updates the specified firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to update.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<FirewallRule> updateAsync(String resourceGroupName, String accountName, String firewallRuleName, final ServiceCallback<FirewallRule> serviceCallback);

    /**
     * Updates the specified firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to update.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the FirewallRule object
     */
    Observable<FirewallRule> updateAsync(String resourceGroupName, String accountName, String firewallRuleName);

    /**
     * Updates the specified firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to update.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the FirewallRule object
     */
    Observable<ServiceResponse<FirewallRule>> updateWithServiceResponseAsync(String resourceGroupName, String accountName, String firewallRuleName);
    /**
     * Updates the specified firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to update.
     * @param parameters Parameters supplied to update the firewall rule.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the FirewallRule object if successful.
     */
    FirewallRule update(String resourceGroupName, String accountName, String firewallRuleName, UpdateFirewallRuleParameters parameters);

    /**
     * Updates the specified firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to update.
     * @param parameters Parameters supplied to update the firewall rule.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<FirewallRule> updateAsync(String resourceGroupName, String accountName, String firewallRuleName, UpdateFirewallRuleParameters parameters, final ServiceCallback<FirewallRule> serviceCallback);

    /**
     * Updates the specified firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to update.
     * @param parameters Parameters supplied to update the firewall rule.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the FirewallRule object
     */
    Observable<FirewallRule> updateAsync(String resourceGroupName, String accountName, String firewallRuleName, UpdateFirewallRuleParameters parameters);

    /**
     * Updates the specified firewall rule.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to update.
     * @param parameters Parameters supplied to update the firewall rule.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the FirewallRule object
     */
    Observable<ServiceResponse<FirewallRule>> updateWithServiceResponseAsync(String resourceGroupName, String accountName, String firewallRuleName, UpdateFirewallRuleParameters parameters);

    /**
     * Deletes the specified firewall rule from the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to delete.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     */
    void delete(String resourceGroupName, String accountName, String firewallRuleName);

    /**
     * Deletes the specified firewall rule from the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to delete.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<Void> deleteAsync(String resourceGroupName, String accountName, String firewallRuleName, final ServiceCallback<Void> serviceCallback);

    /**
     * Deletes the specified firewall rule from the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to delete.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    Observable<Void> deleteAsync(String resourceGroupName, String accountName, String firewallRuleName);

    /**
     * Deletes the specified firewall rule from the specified Data Lake Analytics account.
     *
     * @param resourceGroupName The name of the Azure resource group.
     * @param accountName The name of the Data Lake Analytics account.
     * @param firewallRuleName The name of the firewall rule to delete.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponse} object if successful.
     */
    Observable<ServiceResponse<Void>> deleteWithServiceResponseAsync(String resourceGroupName, String accountName, String firewallRuleName);

    /**
     * Lists the Data Lake Analytics firewall rules within the specified Data Lake Analytics account.
     *
     * @param nextPageLink The NextLink from the previous successful call to List operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws CloudException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the PagedList&lt;FirewallRule&gt; object if successful.
     */
    PagedList<FirewallRule> listByAccountNext(final String nextPageLink);

    /**
     * Lists the Data Lake Analytics firewall rules within the specified Data Lake Analytics account.
     *
     * @param nextPageLink The NextLink from the previous successful call to List operation.
     * @param serviceFuture the ServiceFuture object tracking the Retrofit calls
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    ServiceFuture<List<FirewallRule>> listByAccountNextAsync(final String nextPageLink, final ServiceFuture<List<FirewallRule>> serviceFuture, final ListOperationCallback<FirewallRule> serviceCallback);

    /**
     * Lists the Data Lake Analytics firewall rules within the specified Data Lake Analytics account.
     *
     * @param nextPageLink The NextLink from the previous successful call to List operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the PagedList&lt;FirewallRule&gt; object
     */
    Observable<Page<FirewallRule>> listByAccountNextAsync(final String nextPageLink);

    /**
     * Lists the Data Lake Analytics firewall rules within the specified Data Lake Analytics account.
     *
     * @param nextPageLink The NextLink from the previous successful call to List operation.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the PagedList&lt;FirewallRule&gt; object
     */
    Observable<ServiceResponse<Page<FirewallRule>>> listByAccountNextWithServiceResponseAsync(final String nextPageLink);

}
