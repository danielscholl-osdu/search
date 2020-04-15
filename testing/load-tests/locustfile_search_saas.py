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
from constants import ES_SAAS_QUERY_URL
from google_credentials import get_app_default_access_token
from locust import HttpLocust, TaskSet, task
import elastic_query
import json


class UserBehavior(TaskSet):
    @task(4)
    def post_search(self):
        bearer_token = get_app_default_access_token()
        headers = {'Content-type': 'application/json', 'Accept': 'application/json', 'Authorization': bearer_token}

        kind_query = elastic_query.get_kind_query()
        json_body = json.dumps(kind_query)

        self.client.post("", data=json_body, headers=headers)


class WebsiteUser(HttpLocust):
    host = ES_SAAS_QUERY_URL
    task_set = UserBehavior

    min_wait = 5000
    max_wait = 9000
