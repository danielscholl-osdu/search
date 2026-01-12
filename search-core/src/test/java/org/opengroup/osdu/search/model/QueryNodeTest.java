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

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class QueryNodeTest {

    @Test
    void toQueryBuilder_usesProvidedQuery_afterTrimmingLeadingSpaces() {
        QueryNode node = new QueryNode("   title:foo", "AND");

        Query built = node.toQueryBuilder().build();
        QueryStringQuery qs = built.queryString();

        assertNotNull(qs, "Expected query_string query");
        assertEquals("title:foo", qs.query(), "Leading spaces should be trimmed");
        // sanity: important default in production code
        assertFalse(Boolean.TRUE.equals(qs.allowLeadingWildcard()),
                "allowLeadingWildcard should be false");
    }

    @Test
    void toQueryBuilder_defaultsToWildcard_whenNullOrBlank() {
        QueryNode nullNode = new QueryNode(null, null);
        Query q1 = nullNode.toQueryBuilder().build();
        assertNotNull(q1.queryString());
        assertEquals("*", q1.queryString().query());

        QueryNode blankNode = new QueryNode("   ", "OR");
        Query q2 = blankNode.toQueryBuilder().build();
        assertNotNull(q2.queryString());
        assertEquals("*", q2.queryString().query());
    }

    @Test
    void operator_fromValue_isCaseInsensitive_andUnknownIsNull() {
        assertEquals(QueryNode.Operator.AND, QueryNode.Operator.fromValue("AND"));
        assertEquals(QueryNode.Operator.OR,  QueryNode.Operator.fromValue("or"));
        assertEquals(QueryNode.Operator.NOT, QueryNode.Operator.fromValue("NoT"));
        assertNull(QueryNode.Operator.fromValue("XOR"),
                "Unknown operator should return null");
    }

    @Test
    void constructor_setsOperatorParsedFromString() {
        QueryNode node = new QueryNode("foo:bar", "or");
        assertEquals(QueryNode.Operator.OR, node.getOperator());

        QueryNode node2 = new QueryNode("foo:bar", "unknown");
        assertNull(node2.getOperator(), "Unknown operator should remain null");
    }
}
