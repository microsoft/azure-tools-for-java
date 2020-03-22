/*
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
 */
package com.microsoft.azuretools.authmanage.srvpri.step;


import com.microsoft.azuretools.authmanage.srvpri.entities.RoleAssignment;
import com.microsoft.azuretools.authmanage.srvpri.entities.RoleAssignmentRet;
import com.microsoft.azuretools.authmanage.srvpri.entities.RoleDefinitionRet;
import com.microsoft.azuretools.authmanage.srvpri.exceptions.AzureException;
import com.microsoft.azuretools.authmanage.srvpri.report.Reporter;
import com.microsoft.azuretools.authmanage.srvpri.rest.ArmRestHelper;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.azuretools.utils.Pair;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by shch on 8/21/2016.
 */
public class RoleAssignmentStep implements IStep {
    private ArmRestHelper armRestHelper;
    private Reporter<String> reporter;
    private List<Pair<String, String>> roleAssignmentNames = new LinkedList<>();
    private final AccessTokenAzureManager preAccessTokenAzureManager;

    public RoleAssignmentStep(AccessTokenAzureManager preAccessTokenAzureManager) {
        this.preAccessTokenAzureManager = preAccessTokenAzureManager;
    }

    @Override
    public void execute(Map<String, Object> params) throws IOException, InterruptedException {
        String roleDefinitionName = "Contributor";
        String tenantId = CommonParams.getTenantId();
        armRestHelper = new ArmRestHelper(preAccessTokenAzureManager, tenantId);
        UUID spObjectId = (UUID)params.get("spObjectId");

        reporter = CommonParams.getReporter();
        reporter.report("spObjectId: " + spObjectId);
        params.put("roleAssignmentNames", roleAssignmentNames);

        for (String subscriptionId : CommonParams.getSubscriptionIdList()) {
            String statusActionName = String.format(getName(), subscriptionId);
            try {
                reporter.report("==> For subscriptionId: " + subscriptionId);
                reporter.report("\tGetting role definition...");
                RoleDefinitionRet rd = getRoleDefinition(roleDefinitionName, subscriptionId);
                String roleDefinitionId = rd.value.get(0).id;

                reporter.report("\tAssigning role...");

                final int RETRY_QNTY = 5;
                final int SLEEP_SEC = 10;
                int retry_count = 0;
                while (retry_count < RETRY_QNTY) {
                    try {
                        RoleAssignmentRet ra = assignRole(roleDefinitionId, spObjectId, subscriptionId);
                        String roleAssignmentName = ra.name;
                        reporter.report("SUCCESSFUL: roleAssignmentName: " +  roleAssignmentName);
                        roleAssignmentNames.add(new Pair<String, String>(roleAssignmentName, subscriptionId));
//                        params.put("roleAssignmentNames", roleAssignmentNames);
                        break;
                    } catch (AzureException e) {
                        if (e.getCode().equals("PrincipalNotFound")) {
                            retry_count++;
                            if ((retry_count >= RETRY_QNTY)) {
                                throw e;
                            }
                            reporter.report(String.format("\tFailed! Will retry in %d sec...", SLEEP_SEC));
                            Thread.sleep(SLEEP_SEC * 1000);
                        } else {
                            throw e;
                        }
                    }
                }

                CommonParams.getStatusReporter().report(new Status(
                        statusActionName,
                        Status.Result.SUCCESSFUL,
                        "Assigned as " + roleDefinitionName
                ));
                CommonParams.getResultSubscriptionIdList().add(subscriptionId);

            } catch (AzureException ex) {
                String errorDetails = "(" + ex.getCode() + ") " + ex.getDescription();
                reporter.report("ERROR:" + errorDetails);
                CommonParams.getStatusReporter().report(new Status(
                        statusActionName,
                        Status.Result.FAILED,
                        errorDetails
                ));
            }
        }
        if (!CommonParams.getResultSubscriptionIdList().isEmpty()) {
            params.put("status", "done");
        }
    }

    @Override
    public void rollback(Map<String, Object> params) throws IOException {
        for (Pair<String, String> rap : roleAssignmentNames) {
            String roleAssignmentName = rap.first();
            String sid = rap.second();
            reporter.report("deleting role assignment: " + roleAssignmentName);
            resignRole(roleAssignmentName, sid);
        }
    }

    @Override
    public String getName() {
        return "Role assignment for subscription %s";
    }

    // helpers
    // get Role definition
    public RoleDefinitionRet getRoleDefinition(String roleName, String subscriptionId) throws IOException {

        String resp = armRestHelper.doGet(
                subscriptionId + "/providers/Microsoft.Authorization/roleDefinitions",
                String.format("$filter=roleName+eq+'%s'", roleName));

        ObjectMapper mapper = new ObjectMapper();
        RoleDefinitionRet rd = mapper.readValue(resp, RoleDefinitionRet.class);
        return rd;
    }

    // Assign role
    public RoleAssignmentRet assignRole(String roleDefinitionId, UUID principalId, String subscriptionId) throws IOException {
        /*
            PUT https://management.azure.com//subscriptions/9657ab5d-4a4a-4fd2-ae7a-4cd9fbd030ef/providers/Microsoft.Authorization/roleAssignments/25a768e8-523b-4e39-a520-f6978657ffb7?api-version=2015-07-01
            {
                "properties": {
                    "roleDefinitionId": "/subscriptions/9657ab5d-4a4a-4fd2-ae7a-4cd9fbd030ef/providers/Microsoft.Authorization/roleDefinitions/b24988ac-6180-42a0-ab88-20f7382dd24c",
                    "principalId": "f144ba9d-f4af-48b8-a992-3e328f2710b9"
                }
            }
            */

        RoleAssignment ra = new RoleAssignment();
        ra.properties.roleDefinitionId = roleDefinitionId;
        ra.properties.principalId = principalId;
        ObjectMapper mapper = new ObjectMapper();
        String raJson = mapper.writeValueAsString(ra);

        String resp = armRestHelper.doPut(
                subscriptionId + "/providers/Microsoft.Authorization/roleAssignments/" + UUID.randomUUID(),
                null,
                raJson
            );

        RoleAssignmentRet rar = mapper.readValue(resp, RoleAssignmentRet.class);
        return rar;
    }

    private void resignRole(String roleAssignmentName, String subscriptionId) throws IOException {
        /*
            DELETE https://management.azure.com//subscriptions/9657ab5d-4a4a-4fd2-ae7a-4cd9fbd030ef/providers/Microsoft.Authorization/roleAssignments/25a768e8-523b-4e39-a520-f6978657ffb7?api-version=2015-07-01
         */

        @SuppressWarnings("unused")
        String resp = armRestHelper.doDelete(
                subscriptionId + "/providers/Microsoft.Authorization/roleAssignments/" + roleAssignmentName,
                null,
                null
            );
    }

    @SuppressWarnings("unused")
    private static RoleAssignmentRet getRoleAsiignment(String roleAsiignmetnName, UUID subscriptionId) {
        //TODO.shch: implement
        return null;
    }

}
