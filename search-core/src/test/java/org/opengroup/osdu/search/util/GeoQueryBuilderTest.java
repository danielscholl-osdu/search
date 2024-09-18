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

package org.opengroup.osdu.search.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.elasticsearch.geometry.Circle;
import org.elasticsearch.geometry.Rectangle;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;

@RunWith(MockitoJUnitRunner.class)
@Ignore
//TODO:
public class GeoQueryBuilderTest {

    private static final String GEO_FIELD = "data.SpatialLocation.Wgs84Coordinates";
    @InjectMocks
    private GeoQueryBuilder sut;

    //TODO: rewrite tests. ElasticSearch newClient
//    @Test
//    public void should_provide_valid_distanceQuery() throws IOException {
//        SpatialFilter spatialFilter = new SpatialFilter();
//        spatialFilter.setField(GEO_FIELD);
//        SpatialFilter.ByDistance circl = new SpatialFilter.ByDistance();
//        Point center = new Point(1.02, -8.61);
//        circl.setDistance(1000);
//        circl.setPoint(center);
//        spatialFilter.setByDistance(circl);
//
//        QueryBuilder queryBuilder = this.sut.getGeoQuery(spatialFilter);
//
//        assertNotNull(queryBuilder);
//        GeoShapeQueryBuilder shapeQueryBuilder = (GeoShapeQueryBuilder) queryBuilder;
//        assertEquals(GEO_FIELD, shapeQueryBuilder.fieldName());
//        assertEquals(true, shapeQueryBuilder.ignoreUnmapped());
//        assertTrue(shapeQueryBuilder.shape() instanceof Circle);
//        assertEquals("WITHIN", shapeQueryBuilder.relation().name());
//        Circle circle = (Circle) shapeQueryBuilder.shape();
//        assertEquals(-8.61, circle.getX(), 0.000001);
//        assertEquals(1.02, circle.getY(), 0.000001);
//    }

    //TODO: rewrite tests. ElasticSearch newClient
//    @Test
//    public void should_provide_valid_boundingBoxQuery() throws IOException {
//        SpatialFilter spatialFilter = new SpatialFilter();
//        spatialFilter.setField(GEO_FIELD);
//        SpatialFilter.ByBoundingBox boundingBox = new SpatialFilter.ByBoundingBox();
//        Point topLeft = new Point(90, -180);
//        Point bottomRight = new Point(-90, 180);
//        boundingBox.setTopLeft(topLeft);
//        boundingBox.setBottomRight(bottomRight);
//        spatialFilter.setByBoundingBox(boundingBox);
//
//        QueryBuilder queryBuilder = this.sut.getGeoQuery(spatialFilter);
//
//        assertNotNull(queryBuilder);
//        GeoShapeQueryBuilder shapeQueryBuilder = (GeoShapeQueryBuilder) queryBuilder;
//        assertEquals(GEO_FIELD, shapeQueryBuilder.fieldName());
//        assertEquals(true, shapeQueryBuilder.ignoreUnmapped());
//        assertTrue(shapeQueryBuilder.shape() instanceof Rectangle);
//        assertEquals("WITHIN", shapeQueryBuilder.relation().name());
//        Rectangle rectangle = (Rectangle) shapeQueryBuilder.shape();
//        assertEquals(90, rectangle.getMaxLat(), 0.01);
//        assertEquals(180, rectangle.getMaxLon(), 0.01);
//        assertEquals(-90, rectangle.getMinLat(), 0.01);
//        assertEquals(-180, rectangle.getMinLon(), 0.01);
//    }

    //TODO: rewrite tests. ElasticSearch newClient
//    @Test
//    public void should_provide_valid_polygonQuery() throws IOException {
//        SpatialFilter spatialFilter = new SpatialFilter();
//        spatialFilter.setField(GEO_FIELD);
//        SpatialFilter.ByGeoPolygon geoPolygon = new SpatialFilter.ByGeoPolygon();
//        Point point = new Point(1.02, -8.61);
//        Point point1 = new Point(1.02, -2.48);
//        Point point2 = new Point(10.74, -2.48);
//        Point point3 = new Point(10.74, -8.61);
//        Point point4 = new Point(1.02, -8.61);
//        List<Point> points = new ArrayList<>();
//        points.add(point);
//        points.add(point1);
//        points.add(point2);
//        points.add(point3);
//        points.add(point4);
//        geoPolygon.setPoints(points);
//        spatialFilter.setByGeoPolygon(geoPolygon);
//
//        QueryBuilder queryBuilder = this.sut.getGeoQuery(spatialFilter);
//
//        assertNotNull(queryBuilder);
//        GeoShapeQueryBuilder shapeQueryBuilder = (GeoShapeQueryBuilder) queryBuilder;
//        assertEquals(GEO_FIELD, shapeQueryBuilder.fieldName());
//        assertEquals(true, shapeQueryBuilder.ignoreUnmapped());
//        assertTrue(shapeQueryBuilder.shape() instanceof org.elasticsearch.geometry.Polygon);
//        assertEquals("WITHIN", shapeQueryBuilder.relation().name());
//        org.elasticsearch.geometry.Polygon expectedPolygon = (org.elasticsearch.geometry.Polygon) shapeQueryBuilder.shape();
//        assertArrayEquals(new double[]{1.02, 1.02, 10.74, 10.74, 1.02}, expectedPolygon.getPolygon().getLats(), 0.01);
//        assertArrayEquals(new double[]{-8.61, -2.48, -2.48, -8.61, -8.61}, expectedPolygon.getPolygon().getLons(), 0.01);
//    }

    //TODO: rewrite tests. ElasticSearch newClient
//    @Test
//    public void should_provide_valid_intersectionQuery() throws IOException {
//        SpatialFilter spatialFilter = new SpatialFilter();
//        spatialFilter.setField(GEO_FIELD);
//        SpatialFilter.ByIntersection byIntersection = new SpatialFilter.ByIntersection();
//        Polygon polygon = new Polygon();
//        Point point = new Point(1.02, -8.61);
//        Point point1 = new Point(1.02, -2.48);
//        Point point2 = new Point(10.74, -2.48);
//        Point point3 = new Point(10.74, -8.61);
//        Point point4 = new Point(1.02, -8.61);
//        List<Point> points = new ArrayList<>();
//        points.add(point);
//        points.add(point1);
//        points.add(point2);
//        points.add(point3);
//        points.add(point4);
//        polygon.setPoints(points);
//        List<Polygon> polygons = new ArrayList<>();
//        polygons.add(polygon);
//        byIntersection.setPolygons(polygons);
//        spatialFilter.setByIntersection(byIntersection);
//
//        QueryBuilder queryBuilder = this.sut.getGeoQuery(spatialFilter);
//
//        assertNotNull(queryBuilder);
//        GeoShapeQueryBuilder shapeQueryBuilder = (GeoShapeQueryBuilder) queryBuilder;
//        assertEquals(GEO_FIELD, shapeQueryBuilder.fieldName());
//        assertEquals(true, shapeQueryBuilder.ignoreUnmapped());
//        assertEquals("INTERSECTS", shapeQueryBuilder.relation().name());
//        assertTrue(shapeQueryBuilder.shape() instanceof org.elasticsearch.geometry.GeometryCollection);
//        org.elasticsearch.geometry.GeometryCollection geometryCollection = (org.elasticsearch.geometry.GeometryCollection) shapeQueryBuilder.shape();
//        org.elasticsearch.geometry.MultiPolygon multiPolygon = (org.elasticsearch.geometry.MultiPolygon) geometryCollection.get(0);
//        assertArrayEquals(new double[]{1.02, 1.02, 10.74, 10.74, 1.02}, multiPolygon.get(0).getPolygon().getLats(), 0.01);
//        assertArrayEquals(new double[]{-8.61, -2.48, -2.48, -8.61, -8.61}, multiPolygon.get(0).getPolygon().getLons(), 0.01);
//    }
}
