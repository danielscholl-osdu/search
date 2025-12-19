package org.opengroup.osdu.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.opengroup.osdu.core.common.model.search.Point;
import org.opengroup.osdu.core.common.model.search.Polygon;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class SpatialFilter {
    String field;
    ByBoundingBox byBoundingBox;
    ByDistance byDistance;
    ByGeoPolygon byGeoPolygon;
    ByIntersection byIntersection;
    ByWithinPolygon byWithinPolygon;

    @Builder
    public static class ByDistance {
        Points point;
        int distance;
    }

    @Builder
    public static class ByBoundingBox {
        Points topLeft;
        Points bottomRight;
    }

    @Builder
    public static class Points {
        Double latitude;
        Double longitude;
    }

    @Builder
    public static class ByGeoPolygon {
        List<Points> points;
    }

    @Builder
    public static class ByIntersection {
        private List<Polygon> polygons;
    }

    @Builder
    public static class ByWithinPolygon {
        private List<Point> points;
    }
}