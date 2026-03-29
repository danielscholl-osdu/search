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

package org.opengroup.osdu.search.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class InnerQueryNodeTest {

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
    void toQueryBuilder_withAndOperator_addsMustClause() {
        QueryNode andNode = termNode("AND", "field1", "value1");
        InnerQueryNode inner = new InnerQueryNode(null, List.of(andNode));

        Query result = inner.toQueryBuilder().build();

        assertNotNull(result.bool());
        BoolQuery bq = result.bool();
        assertEquals(1, bq.must().size());
        assertTrue(bq.should().isEmpty());
        assertTrue(bq.mustNot().isEmpty());
    }

    @Test
    void toQueryBuilder_withOrOperator_addsShouldClause() {
        QueryNode orNode = termNode("OR", "field1", "value1");
        InnerQueryNode inner = new InnerQueryNode(null, List.of(orNode));

        Query result = inner.toQueryBuilder().build();

        assertNotNull(result.bool());
        BoolQuery bq = result.bool();
        assertEquals(1, bq.should().size());
        assertTrue(bq.must().isEmpty());
        assertTrue(bq.mustNot().isEmpty());
    }

    @Test
    void toQueryBuilder_withNotOperator_addsMustNotClause() {
        QueryNode notNode = termNode("NOT", "field1", "value1");
        InnerQueryNode inner = new InnerQueryNode(null, List.of(notNode));

        Query result = inner.toQueryBuilder().build();

        assertNotNull(result.bool());
        BoolQuery bq = result.bool();
        assertEquals(1, bq.mustNot().size());
        assertTrue(bq.must().isEmpty());
        assertTrue(bq.should().isEmpty());
    }

    @Test
    void toQueryBuilder_withNullOperator_defaultsToAnd() {
        QueryNode defaultNode = termNode(null, "field1", "value1");
        InnerQueryNode inner = new InnerQueryNode(null, List.of(defaultNode));

        Query result = inner.toQueryBuilder().build();

        assertNotNull(result.bool());
        BoolQuery bq = result.bool();
        assertEquals(1, bq.must().size(), "null operator should default to AND (must)");
    }

    @Test
    void toQueryBuilder_withMixedOperators_buildsCorrectBoolQuery() {
        QueryNode andNode = termNode("AND", "f1", "v1");
        QueryNode orNode = termNode("OR", "f2", "v2");
        QueryNode notNode = termNode("NOT", "f3", "v3");

        InnerQueryNode inner = new InnerQueryNode(null, Arrays.asList(andNode, orNode, notNode));

        Query result = inner.toQueryBuilder().build();
        BoolQuery bq = result.bool();

        assertEquals(1, bq.must().size());
        assertEquals(1, bq.should().size());
        assertEquals(1, bq.mustNot().size());
        assertEquals(1.0f, bq.boost(), 0.0f);
    }

    @Test
    void toQueryBuilder_withNullInnerNodes_usesQueryStringWithWildcard() {
        InnerQueryNode inner = new InnerQueryNode(null, null);

        Query result = inner.toQueryBuilder().build();

        assertNotNull(result.queryString());
        assertEquals("*", result.queryString().query());
        assertFalse(result.queryString().allowLeadingWildcard());
    }

    @Test
    void toQueryBuilder_withEmptyInnerNodes_usesQueryStringWithWildcard() {
        InnerQueryNode inner = new InnerQueryNode(null, Collections.emptyList());

        Query result = inner.toQueryBuilder().build();

        assertNotNull(result.queryString());
        assertEquals("*", result.queryString().query());
    }
}


