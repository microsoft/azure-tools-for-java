# Account API 
> version: `stable-2016-11-01`

> date: `2018-05-18`

## Prerequisites
We depend `azure-rest-api-specs-pr` and `azure-libraries-for-java` to generate Java API. So make sure you have these files in current directory before running `autorest`.

> 1. `git clone https://github.com/hnihanth/azure-rest-api-specs-pr.git`

> 2. `git clone https://github.com/Azure/azure-libraries-for-java.git`

## Getting Started 

To build the SDKs for Account API, simply install AutoRest via `npm` (`npm install -g autorest`) and then run:
> `cd azure-rest-api-specs-pr/specification/datalake-analytics/resource-manager/Microsoft.DataLakeAnalytics/stable/2016-11-01`
> `autorest readme.md`

To see additional help and options, run:
> `autorest --help`

For other options on installation see [Installing AutoRest](https://aka.ms/autorest/install) on the AutoRest github page.

> see https://aka.ms/autorest for more details about autorest.

---

## Configuration 
The following are the settings for this using this API with AutoRest.

``` yaml
# specify the version of Autorest to use
version: 2.0.4278
input-file: account.json
java:
  azure-arm: true
  namespace: com.microsoft.azure.hdinsight.sdk.client.azure.datalake.accounts
  payload-flattening-threshold: 1
  azure-libraries-for-java-folder: ../../../../../../../azure-libraries-for-java
  output-folder: account
  max-comment-columns: 120
  license-header: |
    Copyright (c) Microsoft Corporation
    
    All rights reserved.
    
    MIT License
    
    Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
    documentation files (the "Software"), to deal in the Software without restriction, including without limitation
    the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
    to permit persons to whom the Software is furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all copies or substantial portions of
    the Software.
    
    THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
    THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
    TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
```

