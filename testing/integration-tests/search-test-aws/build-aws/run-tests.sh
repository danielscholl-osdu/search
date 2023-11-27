# Copyright © 2020 Amazon Web Services
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This script executes the test and copies reports to the provided output directory
# To call this script from the service working directory
# ./dist/testing/integration/build-aws/run-tests.sh "./reports/"


SCRIPT_SOURCE_DIR=$(dirname "$0")
echo "Script source location"
echo "$SCRIPT_SOURCE_DIR"
(cd "$SCRIPT_SOURCE_DIR"/../bin && ./install-deps.sh)

#### ADD REQUIRED ENVIRONMENT VARIABLES HERE ###############################################
# The following variables are automatically populated from the environment during integration testing
# see os-deploy-aws/build-aws/integration-test-env-variables.py for an updated list

# AWS_COGNITO_CLIENT_ID
# ELASTIC_HOST
# ELASTIC_PORT
# FILE_URL
# LEGAL_URL
# SEARCH_URL
# STORAGE_URL
export HOST=$SCHEMA_URL
export SEARCH_HOST=$SEARCH_URL
export STORAGE_HOST=$STORAGE_URL
export OTHER_RELEVANT_DATA_COUNTRIES=US
export DEFAULT_DATA_PARTITION_ID_TENANT1=opendes
export DEFAULT_DATA_PARTITION_ID_TENANT2=common
export ENTITLEMENTS_DOMAIN=example.com
export AWS_COGNITO_AUTH_FLOW=USER_PASSWORD_AUTH
export AWS_COGNITO_AUTH_PARAMS_PASSWORD=$ADMIN_PASSWORD
export AWS_COGNITO_AUTH_PARAMS_USER=$ADMIN_USER
export AWS_COGNITO_AUTH_PARAMS_USER_NO_ACCESS=$USER_NO_ACCESS

export ELASTIC_HOST=$ELASTIC_HOST
export ELASTIC_PORT=$ELASTIC_PORT
export ELASTIC_PASSWORD=$ELASTIC_PASSWORD
export ELASTIC_USER_NAME=$ELASTIC_USERNAME

timestamp=$(date +%s)
export LEGAL_TAG=opendes-public-usa-dataset-1-$timestamp
#### POPULATE LEGAL TAGS #########################################################################
pip3 install -r $SCRIPT_SOURCE_DIR/requirements.txt
token=$(python3 $SCRIPT_SOURCE_DIR/aws_jwt_client.py)
echo '**** Generating token *****************'
echo 'Register Legal tag before Integration Tests ...'
curl --location --request POST "$LEGAL_URL"'legaltags' \
  --header 'accept: application/json' \
  --header 'authorization: Bearer '"$token" \
  --header 'content-type: application/json' \
  --header 'data-partition-id: opendes' \
  --data '{
        "name": "public-usa-dataset-1-'$timestamp'",
        "description": "test legal tag for search integration tests",
        "properties": {
            "countryOfOrigin":["US"],
            "contractId":"A1234",
            "expirationDate":"2099-01-25",
            "dataType":"Public Domain Data",
            "originator":"Default",
            "securityClassification":"Public",
            "exportClassification":"EAR99",
            "personalData":"No Personal Data"
        }
}'

#### RUN INTEGRATION TEST #########################################################################
JAVA_HOME=$JAVA17_HOME

mvn -ntp test -f "$SCRIPT_SOURCE_DIR"/../pom.xml -Dcucumber.options="--plugin junit:target/junit-report.xml"
TEST_EXIT_CODE=$?

#### COPY TEST REPORTS #########################################################################

if [ -n "$1" ]
  then
    cp "$SCRIPT_SOURCE_DIR"/../target/junit-report.xml "$1"/os-search-junit-report.xml
fi

exit $TEST_EXIT_CODE


#### DELETE LEGAL TAGS #########################################################################

echo Delete legaltag after Integration Tests...
curl --location --request DELETE "$LEGAL_URL"'legaltags/'$LEGAL_TAG \
--header 'Authorization: Bearer '"$token" \
--header 'data-partition-id: opendes' \
--header 'Content-Type: application/json'
