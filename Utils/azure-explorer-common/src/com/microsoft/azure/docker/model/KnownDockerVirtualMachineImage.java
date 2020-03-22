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
package com.microsoft.azure.docker.model;

import com.microsoft.azure.management.compute.ImageReference;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;

// TBD: This structure can be read from an external file making it easier to edit/update
public enum KnownDockerVirtualMachineImage {
  /** UbuntuServer 16.04LTS. */
  UBUNTU_SERVER_16_04_LTS("Canonical", "UbuntuServer", "16.04.0-LTS", "latest"),
  /** UbuntuServer 14.04LTS. */
  UBUNTU_SERVER_14_04_LTS("Canonical", "UbuntuServer", "14.04.4-LTS", "latest");
//  /** CoreOS CoreOS Stable */
//  COREOS_STABLE_LATEST("CoreOS", "CoreOS", "Stable", "latest" /* 899.17.0 */),
//  /** OpenLogic CentOS 7.2 */
//  OPENLOGIC_CENTOS_7_2("OpenLogic", "CentOS", "7.2", "latest" /* 7.2.20160620 */),
//  /** Ubuntu_Snappy_Core 15.04 */
//  UBUNTU_SNAPPY_CORE_15_04("Canonical", "Ubuntu_Snappy_Core", "15.04", "latest");

  private final String publisher;
  private final String offer;
  private final String sku;
  private final String version;

  KnownDockerVirtualMachineImage(String publisher, String offer, String sku, String version) {
    this.publisher = publisher;
    this.offer = offer;
    this.sku = sku;
    this.version = version;
  }

  /**
   * @return the name of the image publisher
   */
  public String publisher() {
    return publisher;
  }

  /**
   * @return the name of the image offer
   */
  public String offer() {
    return offer;
  }

  /**
   * @return the name of the image SKU
   */
  public String sku() {
    return sku;
  }

  /**
   * @return the name of the image SKU
   */
  public String version() {
    return version;
  }

  public String toString() {return this.name();}

  /**
   * @return the image reference
   */
  public ImageReference imageReference() {
    return new ImageReference()
        .withPublisher(publisher)
        .withOffer(offer)
        .withSku(sku)
        .withVersion(version);
  }

  /**
   * @return the name of the image SKU
   */
  public AzureOSHost getAzureOSHost() {
    return new AzureOSHost(publisher, offer, sku, version);
  }
}
