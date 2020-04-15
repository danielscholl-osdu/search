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

package org.opengroup.osdu.search.provider.gcp.utils;

import com.google.common.base.Strings;
import org.opengroup.osdu.core.gcp.model.AppEngineHeaders;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Random;
import java.util.UUID;

public class TraceIdExtractor {

    /*
     * "X-Cloud-Trace-Context: TRACE_ID/SPAN_ID;o=TRACE_TRUE" e.g. 105445aa7843bc8bf206b120001000/0;o=1"
     * https://cloud.google.com/trace/docs/support
     * */
    public static String getTraceableCloudContext(MultivaluedMap<String, String> requestHeaders) {
        String traceContextHeader = requestHeaders.getFirst(AppEngineHeaders.CLOUD_TRACE_CONTEXT);

        // get new if not found
        if (Strings.isNullOrEmpty(traceContextHeader)) return getNewTraceContext();
        // return as is
        if (traceContextHeader.endsWith(";o=1")) return traceContextHeader;

        String[] traceParts = traceContextHeader.split("[/;]");
        // if there is only trace-id
        if (traceParts.length == 1) return String.format("%s/%s;o=1", traceContextHeader, getNewSpanId());
        // if trace-id and span-id
        if (traceParts.length == 2) return String.format("%s;o=1", traceContextHeader);
        // trace flag is turned off
        return String.format("%s/%s;o=1", traceParts[0], traceParts[1]);
    }

    public static String getTraceId(String traceContextHeader) {
        String[] traceParts = traceContextHeader.split("[/;]");
        return traceParts.length > 0 ? traceParts[0] : getNewTraceId();
    }

    private static String getNewTraceContext() {
        return String.format("%s/%s;o=1", getNewTraceId(), getNewSpanId());
    }

    private static String getNewTraceId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private static String getNewSpanId() {
        return Integer.toUnsignedString(new Random().nextInt());
    }
}
