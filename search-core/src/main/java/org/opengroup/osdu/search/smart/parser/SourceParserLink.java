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

package org.opengroup.osdu.search.smart.parser;

import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.util.KindParser;
import org.opengroup.osdu.search.smart.filters.SourceFilter;
import org.opengroup.osdu.search.smart.models.Filter;

public class SourceParserLink extends ParserLinkBase {

    @Override
    protected String getFilter() {
        return SourceFilter.filterName;
    }

    @Override
    public QueryRequest append(Filter filter, QueryRequest qr) {
        String kind = KindParser.parse(qr.getKind()).get(0);
        qr.setKind(kind.replace("%s", filter.getValue()));
        return qr;
    }
}
