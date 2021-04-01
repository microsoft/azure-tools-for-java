/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.model.vm;


public class VirtualNetwork {
    public final String name;
    public final String addressSpace;
    public final Subnet subnet;

    public VirtualNetwork(String name, String addressSpace, String subnetName, String subnetAddressSpace) {
        this.name = name;
        this.addressSpace = addressSpace;
        this.subnet = new Subnet(subnetName, subnetAddressSpace);
    }

    public static class Subnet {
        public final String name;
        public final String addressSpace;

        public Subnet(String name, String addressSpace) {
            this.name = name;
            this.addressSpace = addressSpace;
        }
    }
}
