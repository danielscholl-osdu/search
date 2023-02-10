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

package org.opengroup.osdu.search.util;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.search.QueryRequest;
import org.opengroup.osdu.core.common.search.ElasticIndexNameResolver;
import org.opengroup.osdu.search.service.IndexAliasService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CrossTenantUtilsTest {
    private static final int MAX_INDEX_NAME_LENGTH = 3840;

    @Mock
    private QueryRequest queryRequest;
    @Mock
    private ElasticIndexNameResolver elasticIndexNameResolver;
    @Mock
    private IndexAliasService indexAliasService;
    @InjectMocks
    private CrossTenantUtils sut;

    @Test
    public void should_returnIndexAsIs_when_searchedCrossKind_given_multipleAccountId() {
        String kind = "*:ihs:well:1.0.0";
        when(queryRequest.getKind()).thenReturn(kind);
        when(this.elasticIndexNameResolver.getIndexNameFromKind(kind)).thenReturn("*-ihs-well-1.0.0");

        assertEquals("*-ihs-well-1.0.0,-.*", this.sut.getIndexName(queryRequest));
    }

    @Test
    public void should_returnMultiIndicesAsIs_when_searchedCrossKind_given_multipleAccountId() {
        ArrayList kind = new ArrayList();
        kind.add("opendes:welldb:wellbore2:1.0.0");
        kind.add("opendes:osdudemo:wellbore:1.0.0");
        kind.add("opendes:wks:polylineSet:1.0.0");
        kind.add("slb:wks:log:1.0.5");
        String indices = "opendes-welldb-wellbore2-1.0.0,opendes-osdudemo-wellbore-1.0.0,opendes-wks-polylineSet-1.0.0,slb-wks-log-1.0.5,-.*";
        when(queryRequest.getKind()).thenReturn(kind);
        mock_getIndexNameFromKind();
        assertEquals(indices, this.sut.getIndexName(queryRequest));
    }

    @Test
    public void should_returnMultiIndicesAsIs_when_searchedCrossKind_separatedByComma_given_multipleAccountId() {
        String kind = "opendes:welldb:wellbore2:1.0.0,opendes:osdudemo:wellbore:1.0.0,opendes:wks:polylineSet:1.0.0,slb:wks:log:1.0.5";
        String indices = "opendes-welldb-wellbore2-1.0.0,opendes-osdudemo-wellbore-1.0.0,opendes-wks-polylineSet-1.0.0,slb-wks-log-1.0.5,-.*";
        when(queryRequest.getKind()).thenReturn(kind);
        mock_getIndexNameFromKind();
        assertEquals(indices, this.sut.getIndexName(queryRequest));
    }

    @Test
    public void should_returnMultiIndicesAsIs_when_total_length_is_not_longer_than_max_length() {
        List<String> kinds = getKindsNotLongerThan(MAX_INDEX_NAME_LENGTH);
        when(queryRequest.getKind()).thenReturn(kinds);
        mock_getIndexNameFromKind();
        assertEquals(getIndexName(kinds), this.sut.getIndexName(queryRequest));
    }

    @Test
    public void should_returnMultiIndices_aliases_when_total_length_is_longer_than_max_length() {
        List<String> kinds = getKindsNotLongerThan(MAX_INDEX_NAME_LENGTH * 2);
        String alias = "a1234567890";
        Map<String, String> kindAliasMap = new HashMap<>();
        kindAliasMap.put(kinds.get(0), alias);

        when(indexAliasService.getIndicesAliases(any())).thenReturn(kindAliasMap);
        when(queryRequest.getKind()).thenReturn(kinds);
        mock_getIndexNameFromKind();
        assertEquals(getIndexName(kinds.size(), alias), this.sut.getIndexName(queryRequest));
    }

    @Test
    public void should_returnMultiIndicesAsIs_when_total_length_is_longer_than_max_length_but_alias_exist() {
        List<String> kinds = getKindsNotLongerThan(MAX_INDEX_NAME_LENGTH * 2);
        Map<String, String> kindAliasMap = new HashMap<>();

        when(indexAliasService.getIndicesAliases(any())).thenReturn(kindAliasMap);
        when(queryRequest.getKind()).thenReturn(kinds);
        mock_getIndexNameFromKind();
        assertEquals(getIndexName(kinds), this.sut.getIndexName(queryRequest));
    }

    private void mock_getIndexNameFromKind() {
        when(this.elasticIndexNameResolver.getIndexNameFromKind(anyString())).thenAnswer(invocation ->
        {
            String kd = invocation.getArgument(0);
            kd = kd.replace(":", "-");
            return kd;
        });
    }

    private List<String> getKindsNotLongerThan(int length) {
        String kind = "osdu:wks:master-data-wellbore:1.0.0";
        ArrayList kinds = new ArrayList();
        int lengthPerKind = kind.length() + 1; // 1 is comma
        int totalLength = "-.*".length();
        int n = 0;
        while (totalLength + lengthPerKind<= length) {
            kinds.add(kind);
            totalLength += lengthPerKind;
        }
        return kinds;
    }

    private String getIndexName(int numberOfAlias, String alias) {
        StringBuilder builder = new StringBuilder();
        int n = 0;
        while(n < numberOfAlias) {
            builder.append(alias);
            builder.append(",");
            n++;
        }
        builder.append("-.*");
        return builder.toString();
    }

    private String getIndexName(List<String> kinds) {
        StringBuilder builder = new StringBuilder();
        for(String kind : kinds) {
            String index = kind.replace(":", "-");
            builder.append(index);
            builder.append(",");
        }
        builder.append("-.*");
        return builder.toString();
    }
}
