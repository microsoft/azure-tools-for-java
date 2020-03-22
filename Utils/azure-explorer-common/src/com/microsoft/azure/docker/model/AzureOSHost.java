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

public class AzureOSHost {
  public String publisher;
  public String sku;
  public String offer;
  public String version;

  public AzureOSHost() {}

  public AzureOSHost(ImageReference imageReference) {
    this.publisher = imageReference.publisher();
    this.offer = imageReference.offer();
    this.sku = imageReference.sku();
    this.version = imageReference.version();
  }

  public AzureOSHost(String publisher, String offer, String sku, String version) {
    this.publisher = publisher;
    this.offer = offer;
    this.sku = sku;
    this.version = version;
  }

  public ImageReference imageReference() {
    return new ImageReference()
        .withPublisher(publisher)
        .withOffer(offer)
        .withSku(sku)
        .withVersion(version);
  }
}
