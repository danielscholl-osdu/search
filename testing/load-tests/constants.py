#!/usr/bin/python
#
# Copyright 2017-2019, Schlumberger
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

import sys


def main(argv):
    pass


if __name__ == '__main__':
    main(sys.argv)
EMAIL_SCOPE = 'https://www.googleapis.com/auth/userinfo.email'
PROFILE_SCOPE = 'https://www.googleapis.com/auth/userinfo.profile'
PROD_QUERY_URL = "https://search-dot-slb-data-lake-prod.appspot.com/api/search/v1/query_with_cursor"
ES_SAAS_QUERY_URL = "https://search-dot-evd-ddl-us-services.appspot.com/api/search/v2/query"
