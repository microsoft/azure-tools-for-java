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
package com.microsoft.azuretools.utils;

import com.microsoft.azure.management.Azure;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.Callable;

public class AzureRegisterProviderNamespaces {
  public static void registerAzureNamespaces(Azure azureInstance) {
    String[] namespaces = new String[] {"Microsoft.Resources", "Microsoft.Network", "Microsoft.Compute",
        "Microsoft.KeyVault", "Microsoft.Storage", "Microsoft.Web", "Microsoft.Authorization", "Microsoft.HDInsight"};
    try {
      Observable.from(namespaces).flatMap(namespace -> {
        return Observable.fromCallable(new Callable<Object>() {
          @Override
          public Object call() throws Exception {
            azureInstance.providers().register(namespace);
            return null;
          }
        }).subscribeOn(Schedulers.io());
      }).toBlocking().subscribe();
//      azureInstance.providers().register("Microsoft.Resources");
//      azureInstance.providers().register("Microsoft.Network");
//      azureInstance.providers().register("Microsoft.Compute");
//      azureInstance.providers().register("Microsoft.KeyVault");
//      azureInstance.providers().register("Microsoft.Storage");
//      azureInstance.providers().register("Microsoft.Web");
//      azureInstance.providers().register("Microsoft.Authorization");
//      azureInstance.providers().register("Microsoft.HDInsight");
    } catch (Exception ignored) {
      // No need to handle this for now since this functionality will be eventually removed once the Azure SDK
      //  something similar
    }
  }
}
