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

package com.microsoft.tooling.msservices.helpers;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExternalStorageHelper {
    private static final String EXTERNAL_STORAGE_LIST = "EXTERNAL_STORAGE_LIST";

    public static List<ClientStorageAccount> getList(Object projectObject) {
        List<ClientStorageAccount> list = new ArrayList<ClientStorageAccount>();

        String[] storageArray = DefaultLoader.getIdeHelper().getProperties(EXTERNAL_STORAGE_LIST, projectObject);
        if (storageArray != null) {

            for (String json : storageArray) {
                ClientStorageAccount clientStorageAccount = new Gson().fromJson(json, ClientStorageAccount.class);
                list.add(clientStorageAccount);
            }

        }

        return list;
    }

    public static void add(ClientStorageAccount clientStorageAccount) {
        String json = new Gson().toJson(clientStorageAccount);

        String[] values = DefaultLoader.getIdeHelper().getProperties(EXTERNAL_STORAGE_LIST);

        ArrayList<String> list = new ArrayList<String>();
        if (values != null) {
            list.addAll(Arrays.asList(values));
        }

        list.add(json);

        DefaultLoader.getIdeHelper().setProperties(EXTERNAL_STORAGE_LIST, list.toArray(new String[list.size()]));
    }

    public static void detach(ClientStorageAccount storageAccount) {
        String[] storageArray = DefaultLoader.getIdeHelper().getProperties(EXTERNAL_STORAGE_LIST);

        if (storageArray != null) {
            ArrayList<String> storageList = Lists.newArrayList(storageArray);

            for (String json : storageArray) {
                ClientStorageAccount csa = new Gson().fromJson(json, ClientStorageAccount.class);

                if (csa.getName().equals(storageAccount.getName())) {
                    storageList.remove(json);
                }
            }

            if (storageList.size() == 0) {
                DefaultLoader.getIdeHelper().unsetProperty(EXTERNAL_STORAGE_LIST);
            } else {
                DefaultLoader.getIdeHelper().setProperties(EXTERNAL_STORAGE_LIST,
                        storageList.toArray(new String[storageList.size()]));
            }
        }
    }
}
