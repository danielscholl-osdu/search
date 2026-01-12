#!/bin/bash
# Copyright © Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

set -e

# Store current directory
CUR_DIR=$(pwd)
SCRIPT_SOURCE_DIR=$(dirname "$0")
cd "$SCRIPT_SOURCE_DIR"


# Required variables for the tests
export AWS_BASE_URL="https://${AWS_DOMAIN}"
export ENTITLEMENTS_DOMAIN=example.com
export TENANT_NAME=opendes
export DATA_PARTITION_ID=int-test-search
export HOST="${AWS_BASE_URL}"
export SEARCH_HOST="${AWS_BASE_URL}/api/search/v2/"
export STORAGE_HOST="${AWS_BASE_URL}/api/storage/v2/"
export INDEXER_HOST="${AWS_BASE_URL}/api/indexer/v2/"
export DEFAULT_DATA_PARTITION_ID_TENANT1=opendes
export DEFAULT_DATA_PARTITION_ID_TENANT2="$AWS_DEFAULT_DATA_PARTITION_ID_TENANT2"
export GROUP_ID="${MY_TENANT}.${DOMAIN}"
export LEGAL_TAG=public-usa-dataset-osduonaws-testing


# Authentication setup
export AWS_COGNITO_AUTH_FLOW="USER_PASSWORD_AUTH"
export PRIVILEGED_USER_TOKEN=$(aws cognito-idp initiate-auth --region "${AWS_REGION}" --auth-flow "${AWS_COGNITO_AUTH_FLOW}" --client-id "${AWS_COGNITO_CLIENT_ID}" --auth-parameters "{\"USERNAME\":\"${ADMIN_USER}\",\"PASSWORD\":\"${ADMIN_PASSWORD}\"}" --query AuthenticationResult.AccessToken --output text)

if [ -z "$PRIVILEGED_USER_TOKEN" ]; then
  echo "ERROR: Failed to obtain PRIVILEGED_USER_TOKEN"
  exit 1
fi


# Test configuration
export TEST_REPLAY_ENABLED=${TEST_REPLAY_ENABLED:-false}
export COLLABORATION_ENABLED=${COLLABORATION_ENABLED:-false}
export OPA_INTEGRATION_ENABLED=${OPA_INTEGRATION_ENABLED:-true}

# Run the tests
mvn clean test
TEST_EXIT_CODE=$?

# Return to original directory
cd "$CUR_DIR"

# Copy test reports if output directory is specified
if [ -n "$1" ]; then
  mkdir -p "$1/search-acceptance-test"
  cp -R "$SCRIPT_SOURCE_DIR/target/surefire-reports/"* "$1/search-acceptance-test"
fi

exit $TEST_EXIT_CODE
