// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

﻿
namespace performance_tests
{
    using System;
    using System.Collections.Generic;
    using System.Text;
    using Microsoft.VisualStudio.TestTools.WebTesting;
    using Google.Apis.Auth.OAuth2;
    using System.Threading.Tasks;

    public class SimpleQuery : WebTest
    {
        private static string token = string.Empty;

        static SimpleQuery()
        {
            if (string.IsNullOrEmpty(token))
            {
                token = GetOrCreateToken().Result;
            }
        }

        public override IEnumerator<WebTestRequest> GetRequestEnumerator()
        {
            WebTestRequest customRequest = new WebTestRequest(Properties.Resources.SandboxQueryUrl);
            customRequest.Headers.Add(new WebTestRequestHeader("Authorization", "Bearer " + token));
            customRequest.Method = "POST";
            StringHttpBody requestBody = new StringHttpBody();
            requestBody.ContentType = "application/json";
            requestBody.InsertByteOrderMark = false;
            requestBody.BodyString = CreateBody();
            customRequest.Body = requestBody;
            customRequest.ExpectedHttpStatusCode = 200;
            yield return customRequest;
        }

        private string CreateBody()
        {
            StringBuilder sb = new StringBuilder();
            sb.Append("{");
            sb.Append("\"kind\":\"ihs:well:1.0.0\"");
            sb.Append(",");
            sb.Append("\"limit\":10");
            sb.Append(",");
            sb.Append("\"query\": \"10\"");
            sb.Append("}");
            return sb.ToString();
        }

        public void PostQueryWithoutLimitTest()
        {
            this.PreAuthenticate = true;
            this.Proxy = "default";
        }

        private static async Task<string> GetOrCreateToken()
        {
            var credential = GoogleCredential.GetApplicationDefault();
            return await credential.UnderlyingCredential.GetAccessTokenForRequestAsync();
        }
    }
}