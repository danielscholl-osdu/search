// Copyright © Schlumberger
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

package org.opengroup.osdu.search.model;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.opengroup.osdu.core.common.model.search.SortQuery;

import java.util.List;
import java.util.Map;


@Data
@AllArgsConstructor
@Builder
public class SearchAfterSettings {
    private String pitId;
    private String userId;
    private SortQuery sortQuery;
    private List<Map.Entry<String, Object>> searchAfterValues;
    private boolean closed;
    private long totalCount;
    public String toString() {
        return (new Gson()).toJson(this);
    }
}


