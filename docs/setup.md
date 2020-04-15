Copyright 2017-2019, Schlumberger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## BYOC Environment Setup
Checkout the code and perform the following actions: 
```shell script
  $ cd os-search  
  $ mvn clean install
  $ java -jar provider/search-byoc/target/search-byoc-1.0-SNAPSHOT-spring-boot.jar  
```
Once the service is up visit browser and hit the url http://localhost:8080/api/search/v2/swagger-ui.html
Enter ```opendes@byoc.local / 123 ``` as username and ***password***

Click on "Search API"

Click on "/query"

Fill in the "queryRequest" textfield with the following contents:

 if you search for 
 ```shell script
 {"kind": "*:*:*:*"}
```
you would get a 200 with no results (that is, if you have no data in memory)

If you search for
```shell script
{
  "kind": "common:ihs:well:1.0.0"
}
```
you will get a 404 which means there is no data found for that kind. 



## Cloud Environment Setup

### Cloud KMS Setup

Enable cloud KMS on master project

Create king ring and key in the ***master project***
```sh
    gcloud services enable cloudkms.googleapis.com
    export KEYRING_NAME="csqp"
    export CRYPTOKEY_NAME="searchService"
    gcloud kms keyrings create $KEYRING_NAME --location global
    gcloud kms keys create $CRYPTOKEY_NAME --location global \
    		--keyring $KEYRING_NAME \
    		--purpose encryption
```

Add **Cloud KMS CryptoKey Encrypter/Decrypter** role to the **App Engine default service account** of the master project through IAM - Role tab

Add "Cloud KMS Encrypt/Decrypt" role to the "App Engine default service account" of ***master project***

Refer to [dps-tenant-init](https://slb-swt.visualstudio.com/data-at-rest/DPS%20Data%20Lake/_git/dps-tenant-init) for new tenant on-boarding script

### Memory Store (Redis Instance) Setup

Create a new Standard tier Redis instance on the ***service project***

The Redis instance must be created under the same region with the App Engine application which needs to access it.

```sh
    gcloud beta redis instances create redis-cache-search --size=10 --region=<service-deployment-region> --zone=<service-deployment-zone> --tier=STANDARD
```
