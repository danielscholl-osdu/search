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

import static org.elasticsearch.index.query.QueryBuilders.geoIntersectionQuery;
import static org.elasticsearch.index.query.QueryBuilders.geoWithinQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.CoordinatesBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.common.geo.builders.GeometryCollectionBuilder;
import org.elasticsearch.common.geo.builders.MultiPolygonBuilder;
import org.elasticsearch.common.geo.builders.PolygonBuilder;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.QueryBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;
import org.opengroup.osdu.core.common.model.search.SpatialFilter;
import org.springframework.stereotype.Component;

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
        List<Point> queryPolygon = spatialFilter.getByGeoPolygon().getPoints();
        List<Coordinate> points = new ArrayList<>();
        if (!queryPolygon.get(0).equals(queryPolygon.get(queryPolygon.size() - 1))) {
            List<Point> closedRing = new ArrayList<>();
            closedRing.addAll(queryPolygon);
            closedRing.add(queryPolygon.get(0));
            spatialFilter.getByGeoPolygon().setPoints(closedRing);
        }
        for (Point point : spatialFilter.getByGeoPolygon().getPoints()) {
            points.add(new Coordinate(point.getLongitude(), point.getLatitude()));
        }
        CoordinatesBuilder cb = new CoordinatesBuilder().coordinates(points);
        return geoWithinQuery(spatialFilter.getField(), new PolygonBuilder(cb)).ignoreUnmapped(true);
    }

    private QueryBuilder getBoundingBoxQuery(SpatialFilter spatialFilter) throws IOException {
        Coordinate topLeft = new Coordinate(spatialFilter.getByBoundingBox().getTopLeft().getLongitude(), spatialFilter.getByBoundingBox().getTopLeft().getLatitude());
        Coordinate bottomRight = new Coordinate(spatialFilter.getByBoundingBox().getBottomRight().getLongitude(), spatialFilter.getByBoundingBox().getBottomRight().getLatitude());
        return geoWithinQuery(spatialFilter.getField(), new EnvelopeBuilder(topLeft, bottomRight)).ignoreUnmapped(true);
    }

    private QueryBuilder getDistanceQuery(SpatialFilter spatialFilter) throws IOException {
        Coordinate center = new Coordinate(spatialFilter.getByDistance().getPoint().getLongitude(), spatialFilter.getByDistance().getPoint().getLatitude());
        CircleBuilder circleBuilder = new CircleBuilder().center(center).radius(spatialFilter.getByDistance().getDistance(), DistanceUnit.METERS);
        return geoWithinQuery(spatialFilter.getField(), circleBuilder).ignoreUnmapped(true);
    }

    private QueryBuilder getIntersectionQuery(SpatialFilter spatialFilter) throws IOException {
        MultiPolygonBuilder multiPolygonBuilder = new MultiPolygonBuilder();
        for (Polygon polygon : spatialFilter.getByIntersection().getPolygons()) {
            List<Coordinate> coordinates = new ArrayList<>();
            for (Point point : polygon.getPoints()) {
                coordinates.add(new Coordinate(point.getLongitude(), point.getLatitude()));
            }

            checkPolygon(coordinates);

            CoordinatesBuilder cb = new CoordinatesBuilder().coordinates(coordinates);
            multiPolygonBuilder.polygon(new PolygonBuilder(cb));
        }

        GeometryCollectionBuilder geometryCollection = new GeometryCollectionBuilder();
        geometryCollection.shape(multiPolygonBuilder);
        return geoIntersectionQuery(spatialFilter.getField(), geometryCollection.buildGeometry()).ignoreUnmapped(true);
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
