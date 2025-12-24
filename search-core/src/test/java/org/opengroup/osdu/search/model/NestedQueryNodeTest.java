// Copyright © Microsoft Corporation
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NestedQueryNodeTest {

    private static QueryNode termNode(String operand, String field, String value) {
        return new QueryNode(null, operand) {
            @Override
            public Query.Builder toQueryBuilder() {

                Query concrete = Query.of(q -> q.term(t -> t.field(field).value(value)));

                Query.Builder mockBuilder = mock(Query.Builder.class);
                when(mockBuilder.build()).thenReturn(concrete);
                return mockBuilder;
            }
        };
    }

    @Test
    void buildsNestedBool_withAndOrNot_andDefault() {

        QueryNode andNode = termNode("AND", "f1", "v1");
        QueryNode orNode  = termNode("OR",  "f2", "v2");
        QueryNode notNode = termNode("NOT", "f3", "v3");
        QueryNode defNode = termNode(null,  "f4", "v4");

        NestedQueryNode nested = new NestedQueryNode(null, null,
                Arrays.asList(andNode, orNode, notNode, defNode), "pathA");

        Query built = nested.toQueryBuilder().build();

        assertNotNull(built.nested(), "Expected a nested query");
        assertEquals("pathA", built.nested().path());
        assertEquals(1.0f, built.nested().boost(), 0.0f);
        assertEquals(ChildScoreMode.Avg, built.nested().scoreMode());
        assertTrue(built.nested().ignoreUnmapped());

        Query inner = built.nested().query();
        assertNotNull(inner.bool(), "Inner query should be BoolQuery");
        BoolQuery bq = inner.bool();

        assertEquals(2, bq.must().size(),    "AND + default(null) should populate 'must'");
        assertEquals(1, bq.should().size(),  "OR should populate 'should'");
        assertEquals(1, bq.mustNot().size(), "NOT should populate 'mustNot'");
        assertEquals(1.0f, bq.boost(), 0.0f);
    }

    @Test
    void emptyInner_buildsNestedQueryString_withProvidedQuery() {
        NestedQueryNode node = new NestedQueryNode("title:osdu", null, Collections.emptyList(), "pathB");

        Query result = node.toQueryBuilder().build();

        assertNotNull(result.nested(), "Expected nested query");
        assertEquals("pathB", result.nested().path());
        assertNotNull(result.nested().query().queryString(), "Fallback should be query_string");
        assertEquals("title:osdu", result.nested().query().queryString().query());
        assertFalse(result.nested().query().queryString().allowLeadingWildcard());
    }

    @Test
    void nullInner_andBlankQuery_defaultsToWildcardStar() throws Exception {

        NestedQueryNode node = new NestedQueryNode("", null, null, "pathC");
        Query result = node.toQueryBuilder().build();

        assertNotNull(result.nested());
        assertEquals("pathC", result.nested().path());
        assertNotNull(result.nested().query().queryString());
        assertEquals("*", result.nested().query().queryString().query());
    }

    @Test
    void nullInner_withQueryStringPresent_usesThatValue() throws Exception {
        NestedQueryNode node = new NestedQueryNode("description:well", null, null, "pathD");

        Query result = node.toQueryBuilder().build();

        assertNotNull(result.nested());
        assertNotNull(result.nested().query().queryString());
        assertEquals("description:well", result.nested().query().queryString().query());
    }
}
