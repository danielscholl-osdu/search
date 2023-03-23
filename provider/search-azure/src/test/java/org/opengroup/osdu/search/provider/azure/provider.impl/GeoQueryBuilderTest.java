//  Copyright © Schlumberger
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.search.provider.azure.provider.impl;

import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class GeoQueryBuilderTest {

    @InjectMocks
    private GeoQueryBuilder sut;

    @Test
    public void should_provide_valid_intersectionQuery() throws IOException {
        SpatialFilter spatialFilter = new SpatialFilter();
        spatialFilter.setField("data.Wgs84Coordinates");
        SpatialFilter.ByIntersection byIntersection = new SpatialFilter.ByIntersection();
        Polygon polygon = new Polygon();
        Point point = new Point(1.02, -8.61);
        Point point1 = new Point(1.02, -2.48);
        Point point2 = new Point(10.74, -2.48);
        Point point3 = new Point(10.74, -8.61);
        Point point4 = new Point(1.02, -8.61);
        List<Point> points = new ArrayList<>();
        points.add(point);
        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);
        polygon.setPoints(points);
        List<Polygon> polygons = new ArrayList<>();
        polygons.add(polygon);
        byIntersection.setPolygons(polygons);
        spatialFilter.setByIntersection(byIntersection);

        QueryBuilder queryBuilder = this.sut.getGeoQuery(spatialFilter);
        assertNotNull(queryBuilder);

        GeoShapeQueryBuilder shapeQueryBuilder = (GeoShapeQueryBuilder) queryBuilder;
        assertEquals("data.Wgs84Coordinates", shapeQueryBuilder.fieldName());
        assertEquals(true, shapeQueryBuilder.ignoreUnmapped());
    }
}
