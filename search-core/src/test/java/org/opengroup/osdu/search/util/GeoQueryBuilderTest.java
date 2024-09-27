/*
 *  Copyright 2017-2019 © Schlumberger
 *  Copyright 2020-2024 Google LLC
 *  Copyright 2020-2024 EPAM Systems, Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opengroup.osdu.search.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import co.elastic.clients.elasticsearch._types.GeoShapeRelation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;

@RunWith(MockitoJUnitRunner.class)
public class GeoQueryBuilderTest {
  private static final String GEO_FIELD = "data.SpatialLocation.Wgs84Coordinates";
  private static final String INTERSECTION_QUERY = "intersection-query";
  private static final String POLYGON_QUERY = "polygon-query";
  private static final String BOX_QUERY = "box-query";
  private static final String DISTANCE_QUERY = "distance-query";

  @InjectMocks
  private GeoQueryBuilder sut;

  @Test
  public void should_provide_valid_distanceQuery() throws IOException {
    SpatialFilter spatialFilter = new SpatialFilter();
    spatialFilter.setField(GEO_FIELD);
    SpatialFilter.ByDistance circl = new SpatialFilter.ByDistance();
    Point center = new Point(1.02, -8.61);
    circl.setDistance(1000);
    circl.setPoint(center);
    spatialFilter.setByDistance(circl);

    JsonObject queryJson = getExpectedQuery(DISTANCE_QUERY);
    SearchRequest expectedResult =
        SearchRequest.of(sr -> sr.query(q -> q.withJson(new StringReader(queryJson.toString()))));

    Query actualResult = this.sut.getGeoQuery(spatialFilter);

    assert expectedResult.query() != null;
    assertNotNull(actualResult);
    assertEquals(expectedResult.query().toString(), actualResult.toString());
    assertEquals(GEO_FIELD, actualResult.geoShape().field());
    assertEquals(true, actualResult.geoShape().ignoreUnmapped());
    assertEquals(GeoShapeRelation.Within, actualResult.geoShape().shape().relation());
  }

  @Test
  public void should_provide_valid_boundingBoxQuery() throws IOException {
    SpatialFilter spatialFilter = new SpatialFilter();
    spatialFilter.setField(GEO_FIELD);
    SpatialFilter.ByBoundingBox boundingBox = new SpatialFilter.ByBoundingBox();
    Point topLeft = new Point(90, -180);
    Point bottomRight = new Point(-90, 180);
    boundingBox.setTopLeft(topLeft);
    boundingBox.setBottomRight(bottomRight);
    spatialFilter.setByBoundingBox(boundingBox);

    JsonObject queryJson = getExpectedQuery(BOX_QUERY);
    SearchRequest expectedResult =
        SearchRequest.of(sr -> sr.query(q -> q.withJson(new StringReader(queryJson.toString()))));

    Query actualResult = this.sut.getGeoQuery(spatialFilter);

    assert expectedResult.query() != null;
    assertNotNull(actualResult);
    assertEquals(expectedResult.query().toString(), actualResult.toString());
    assertEquals(GEO_FIELD, actualResult.geoShape().field());
    assertEquals(true, actualResult.geoShape().ignoreUnmapped());
    assertEquals(GeoShapeRelation.Within, actualResult.geoShape().shape().relation());
  }

  @Test
  public void should_provide_valid_polygonQuery() throws IOException {
    SpatialFilter spatialFilter = new SpatialFilter();
    spatialFilter.setField(GEO_FIELD);
    SpatialFilter.ByGeoPolygon geoPolygon = new SpatialFilter.ByGeoPolygon();
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
    geoPolygon.setPoints(points);
    spatialFilter.setByGeoPolygon(geoPolygon);

    JsonObject queryJson = getExpectedQuery(POLYGON_QUERY);
    SearchRequest expectedResult =
        SearchRequest.of(sr -> sr.query(q -> q.withJson(new StringReader(queryJson.toString()))));

    Query actualResult = this.sut.getGeoQuery(spatialFilter);

    assert expectedResult.query() != null;
    assertNotNull(actualResult);
    assertEquals(expectedResult.query().toString(), actualResult.toString());
    assertEquals(GEO_FIELD, actualResult.geoShape().field());
    assertEquals(GeoShapeRelation.Within, actualResult.geoShape().shape().relation());
  }

  @Test
  public void should_provide_valid_intersectionQuery() throws IOException {
    SpatialFilter spatialFilter = new SpatialFilter();
    spatialFilter.setField(GEO_FIELD);
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

    JsonObject queryJson = getExpectedQuery(INTERSECTION_QUERY);
    SearchRequest expectedResult =
        SearchRequest.of(sr -> sr.query(q -> q.withJson(new StringReader(queryJson.toString()))));

    Query actualResult = this.sut.getGeoQuery(spatialFilter);

    assert expectedResult.query() != null;
    assertNotNull(actualResult);
    assertEquals(expectedResult.query().toString(), actualResult.toString());
    assertEquals(GEO_FIELD, actualResult.geoShape().field());
    assertEquals(GeoShapeRelation.Intersects, actualResult.geoShape().shape().relation());
  }

  private JsonObject getExpectedQuery(String queryFile) throws IOException {
    BufferedReader br;
    try (InputStream inStream =
        this.getClass().getResourceAsStream("/testqueries/expected/" + queryFile + ".json")) {
      br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inStream)));
      Gson gson = new Gson();
      JsonReader reader = new JsonReader(br);
      return gson.fromJson(reader, JsonObject.class);
    }
  }
}
