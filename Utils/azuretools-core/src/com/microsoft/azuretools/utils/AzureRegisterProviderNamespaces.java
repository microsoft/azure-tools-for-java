/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
