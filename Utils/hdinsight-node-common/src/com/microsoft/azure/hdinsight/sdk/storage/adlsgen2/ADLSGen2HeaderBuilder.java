/*
 * Copyright (c) Microsoft Corporation
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
package com.microsoft.azure.hdinsight.sdk.storage.adlsgen2;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;

public class ADLSGen2HeaderBuilder {
    public static String groupHeader = "x-ms-group";
    public static String ownerHeader = "x-ms-owner";
    public static String aclHeader = "x-ms-acl";
    public static String defaultAcls = "user::rwx,default:user::rwx,group::rwx,default:group::rwx,other::rwx,default:other::rwx";

    private HeaderGroup headerGroup;
    public ADLSGen2HeaderBuilder(){
        headerGroup = new HeaderGroup();
    }

    public ADLSGen2HeaderBuilder setGroup(String group){
        Header header = new BasicHeader(groupHeader,group);
        headerGroup.updateHeader(header);
        return this;
    }

    public ADLSGen2HeaderBuilder setOwner(String owner){
        Header header = new BasicHeader(ownerHeader,owner);
        headerGroup.updateHeader(header);
        return this;
    }

    public ADLSGen2HeaderBuilder setAcl(String acl){
        Header header = new BasicHeader(aclHeader,acl);
        headerGroup.updateHeader(header);
        return this;
    }

    public HeaderGroup build(){
        return headerGroup;
    }
}
