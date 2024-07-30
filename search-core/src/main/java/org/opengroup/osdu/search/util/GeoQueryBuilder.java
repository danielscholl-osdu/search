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


import jakarta.servlet.http.HttpServletResponse;
import org.elasticsearch.geometry.Circle;
import org.elasticsearch.geometry.GeometryCollection;
import org.elasticsearch.geometry.LinearRing;
import org.elasticsearch.geometry.MultiPolygon;
import org.elasticsearch.geometry.Rectangle;
import org.elasticsearch.index.query.QueryBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.geoIntersectionQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoWithinQuery;

@Component
public final class GeoQueryBuilder {

    private static final int MINIMUM_POLYGON_POINTS_SIZE = 4;

    public QueryBuilder getGeoQuery(SpatialFilter spatialFilter) throws IOException {
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

    private QueryBuilder getPolygonQuery(SpatialFilter spatialFilter) throws IOException {
        List<Coordinate> points = new ArrayList<>();
        for (Point point : spatialFilter.getByGeoPolygon().getPoints()) {
            points.add(new Coordinate(point.getLongitude(), point.getLatitude()));
        }
        double[] x = points.stream().mapToDouble(pt -> pt.x).toArray();
        double[] y = points.stream().mapToDouble(pt -> pt.y).toArray();
        org.elasticsearch.geometry.Polygon polygon = new org.elasticsearch.geometry.Polygon(
                new LinearRing(x, y)
        );
        return geoWithinQuery(spatialFilter.getField(), polygon).ignoreUnmapped(true);
    }

    private QueryBuilder getBoundingBoxQuery(SpatialFilter spatialFilter) throws IOException {
        Rectangle rectangle = new Rectangle(
                spatialFilter.getByBoundingBox().getTopLeft().getLongitude(),
                spatialFilter.getByBoundingBox().getBottomRight().getLongitude(),
                spatialFilter.getByBoundingBox().getBottomRight().getLatitude(),
                spatialFilter.getByBoundingBox().getTopLeft().getLatitude()
        );
        return geoWithinQuery(spatialFilter.getField(), rectangle).ignoreUnmapped(true);
    }

    private QueryBuilder getDistanceQuery(SpatialFilter spatialFilter) throws IOException {
        Circle circle = new Circle(
                spatialFilter.getByDistance().getPoint().getLongitude(),
                spatialFilter.getByDistance().getPoint().getLatitude(),
                spatialFilter.getByDistance().getDistance()
        );
        return geoWithinQuery(spatialFilter.getField(), circle).ignoreUnmapped(true);
    }

    private QueryBuilder getIntersectionQuery(SpatialFilter spatialFilter) throws IOException {
        List<org.elasticsearch.geometry.Polygon> polygons = new ArrayList<>();
        for (Polygon polygon : spatialFilter.getByIntersection().getPolygons()) {
            List<Coordinate> coordinates = new ArrayList<>();
            for (Point point : polygon.getPoints()) {
                coordinates.add(new Coordinate(point.getLongitude(), point.getLatitude()));
            }

            checkPolygon(coordinates);
            double[] x = coordinates.stream().mapToDouble(coord -> coord.x).toArray();
            double[] y = coordinates.stream().mapToDouble(coord -> coord.y).toArray();
            polygons.add(new org.elasticsearch.geometry.Polygon(
                    new LinearRing(x, y)
            ));
        }

        MultiPolygon multiPolygon = new MultiPolygon(polygons);

        GeometryCollection<MultiPolygon> geometryCollection = new GeometryCollection(Collections.singletonList(multiPolygon));
        return geoIntersectionQuery(spatialFilter.getField(), geometryCollection).ignoreUnmapped(true);
    }

    private void checkPolygon(List<Coordinate> coordinates) {
        if (coordinates.size() < MINIMUM_POLYGON_POINTS_SIZE ||
                (
                        coordinates.get(0).x != coordinates.get(coordinates.size() - 1).x
                                || coordinates.get(0).y != coordinates.get(coordinates.size() - 1).y
                )
        ) {
            throw new AppException(HttpServletResponse.SC_BAD_REQUEST, "Bad Request",
                    String.format(
                            "Polygons must have at least %s points and the first point must match the last point",
                            MINIMUM_POLYGON_POINTS_SIZE));
        }
    }
}
