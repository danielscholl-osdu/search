package org.opengroup.osdu.request;

import lombok.*;

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
}