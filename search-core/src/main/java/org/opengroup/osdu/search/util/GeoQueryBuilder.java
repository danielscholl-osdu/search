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

import co.elastic.clients.elasticsearch._types.GeoShapeRelation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.springframework.stereotype.Component;

@Component
public final class GeoQueryBuilder {

  private static final int MINIMUM_POLYGON_POINTS_SIZE = 4;

  public Query getGeoQuery(SpatialFilter spatialFilter) throws IOException {
    if (spatialFilter.getByBoundingBox() != null) {
      return getBoundingBoxQuery(spatialFilter);
    } else if (spatialFilter.getByDistance() != null) {
      return getDistanceQuery(spatialFilter);
    } else if (spatialFilter.getByGeoPolygon() != null) {
      return getPolygonQuery(spatialFilter);
    } else if (spatialFilter.getByIntersection() != null) {
      return getIntersectionQuery(spatialFilter);
    }
    return null;
  }

  private Query getPolygonQuery(SpatialFilter spatialFilter) throws IOException {
    GeoShapeQuery shapeQuery =
        QueryBuilders.geoShape()
            .field(spatialFilter.getField())
            .shape(
                s ->
                    s.shape(JsonData.of(createPolygon(spatialFilter)))
                        .relation(GeoShapeRelation.Within))
            .boost(1.0F)
            .ignoreUnmapped(true)
            .build();
    return shapeQuery._toQuery();
  }

  private Query getBoundingBoxQuery(SpatialFilter spatialFilter) throws IOException {
    GeoShapeQuery shapeQuery =
        QueryBuilders.geoShape()
            .field(spatialFilter.getField())
            .shape(
                s ->
                    s.shape(JsonData.of(createBoundingBoxQuery(spatialFilter)))
                        .relation(GeoShapeRelation.Within))
            .boost(1.0F)
            .ignoreUnmapped(true)
            .build();
    return shapeQuery._toQuery();
  }

  private static Map<String, Object> createBoundingBoxQuery(SpatialFilter spatialFilter) {
    double topLeftLongitude = spatialFilter.getByBoundingBox().getTopLeft().getLongitude();
    double bottomRightLongitude = spatialFilter.getByBoundingBox().getBottomRight().getLongitude();
    double bottomRightLatitude = spatialFilter.getByBoundingBox().getBottomRight().getLatitude();
    double topLeftLatitude = spatialFilter.getByBoundingBox().getTopLeft().getLatitude();
    List<List<Double>> coordinates = new ArrayList<>();

    coordinates.add(List.of(topLeftLongitude, topLeftLatitude));
    coordinates.add(List.of(bottomRightLongitude, bottomRightLatitude));
    Map<String, Object> shapeMap = new HashMap<>();
    shapeMap.put("type", "Envelope");
    shapeMap.put("coordinates", coordinates);
    return shapeMap;
  }

  private Query getDistanceQuery(SpatialFilter spatialFilter) {
    GeoShapeQuery shapeQuery =
        QueryBuilders.geoShape()
            .field(spatialFilter.getField())
            .shape(
                s ->
                    s.shape(JsonData.of(createDistanceQuery(spatialFilter)))
                        .relation(GeoShapeRelation.Within))
            .boost(1.0F)
            .ignoreUnmapped(true)
            .build();
    return shapeQuery._toQuery();
  }

  private static Map<String, Object> createDistanceQuery(SpatialFilter spatialFilter) {
    Map<String, Object> shapeMap = new HashMap<>();
    shapeMap.put("type", "Circle");
    shapeMap.put("radius", spatialFilter.getByDistance().getDistance() + "m");
    shapeMap.put(
        "coordinates",
        List.of(
            spatialFilter.getByDistance().getPoint().getLongitude(),
            spatialFilter.getByDistance().getPoint().getLatitude()));
    return shapeMap;
  }

  private static Map<String, Object> createPolygon(SpatialFilter spatialFilter) {
    List<Point> queryPolygon = spatialFilter.getByGeoPolygon().getPoints();
    if(queryPolygon.size() > 0 && !queryPolygon.get(0).equals(queryPolygon.get(queryPolygon.size() -1))) {
      queryPolygon.add(queryPolygon.get(0));
    }
    List<List<Double>> coordinates = new ArrayList<>();
    for (Point point : queryPolygon) {
      coordinates.add(List.of(point.getLongitude(), point.getLatitude()));
    }
    if (coordinates.size() < MINIMUM_POLYGON_POINTS_SIZE) {
      throw new AppException(
              HttpServletResponse.SC_BAD_REQUEST,
              "Bad Request",
              String.format("Polygons must have at least %d points", MINIMUM_POLYGON_POINTS_SIZE));
    }

    Map<String, Object> shapeMap = new HashMap<>();
    shapeMap.put("type", "Polygon");
    shapeMap.put("coordinates", List.of(coordinates));
    return shapeMap;
  }

  private static Map<String, Object> createMultiPolygon(List<Polygon> polygons) {
    Map<String, Object> shapeMap = new HashMap<>();
    List<Map<String, Object>> polygonsList = new ArrayList<>();

    for (Polygon polygon : polygons) {
      List<Map<String, Double>> coordinatesList = new ArrayList<>();
      for (Point point : polygon.getPoints()) {
        Map<String, Double> coordMap = new HashMap<>();
        coordMap.put("lat", point.getLatitude());
        coordMap.put("lon", point.getLongitude());
        coordinatesList.add(coordMap);
      }

      checkPolygon(coordinatesList);

      Map<String, Object> polygonMap = new HashMap<>();
      polygonMap.put("type", "polygon");
      polygonMap.put("coordinates", Collections.singletonList(coordinatesList));
      polygonsList.add(polygonMap);
    }

    shapeMap.put("type", "geometrycollection");
    shapeMap.put("geometries", polygonsList);

    return shapeMap;
  }

  private Query getIntersectionQuery(SpatialFilter spatialFilter) throws IOException {
    List<Polygon> polygons = spatialFilter.getByIntersection().getPolygons();
    Map<String, Object> geoShapeJson = createMultiPolygon(polygons);

    GeoShapeQuery shapeQuery =
        QueryBuilders.geoShape()
            .field(spatialFilter.getField())
            .shape(s -> s.relation(GeoShapeRelation.Intersects).shape(JsonData.of(geoShapeJson)))
            .ignoreUnmapped(true)
            .build();

    return shapeQuery._toQuery();
  }

  private static void checkPolygon(List<Map<String, Double>> coordinates) {
    // Check if the polygon has enough points
    if (coordinates.size() < MINIMUM_POLYGON_POINTS_SIZE) {
      throw new AppException(
          HttpServletResponse.SC_BAD_REQUEST,
          "Bad Request",
          String.format("Polygons must have at least %d points", MINIMUM_POLYGON_POINTS_SIZE));
    }

    // Check if the polygon is closed
    Map<String, Double> firstPoint = coordinates.get(0);
    Map<String, Double> lastPoint = coordinates.get(coordinates.size() - 1);

    if (!firstPoint.get("lon").equals(lastPoint.get("lon"))
        || !firstPoint.get("lat").equals(lastPoint.get("lat"))) {
      throw new AppException(
          HttpServletResponse.SC_BAD_REQUEST,
          "Bad Request",
          "The first point must match the last point to close the polygon");
    }
  }
}
