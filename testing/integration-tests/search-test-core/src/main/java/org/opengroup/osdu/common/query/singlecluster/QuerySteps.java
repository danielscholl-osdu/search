package org.opengroup.osdu.common.query.singlecluster;

import org.opengroup.osdu.common.QueryBase;
import org.opengroup.osdu.request.SpatialFilter;
import org.opengroup.osdu.util.HTTPClient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class QuerySteps extends QueryBase {

    public QuerySteps(HTTPClient httpClient) {
        super(httpClient);
    }

    public void define_bounding_box_with_points_None_None_and(Double bottomLatitude, Double bottomLongitude) {
        SpatialFilter.Points bottomRight = SpatialFilter.Points.builder().latitude(bottomLatitude).longitude(bottomLongitude).build();
        byBoundingBox = SpatialFilter.ByBoundingBox.builder().topLeft(null).bottomRight(bottomRight).build();
        spatialFilter.setByBoundingBox(byBoundingBox);
    }

    public void define_focus_coordinates_as_and_search_in_a_radius(Double latitude, Double longitude, int distance) {
        SpatialFilter.Points coordinate = SpatialFilter.Points.builder().latitude(latitude).longitude(longitude).build();
        SpatialFilter.ByDistance byDistance = SpatialFilter.ByDistance.builder().distance(distance).point(coordinate).build();
        spatialFilter.setByDistance(byDistance);
    }

    public void define_geo_polygon_with_following_points_points_list(List<String> points) {
        Pattern pattern = Pattern.compile("[(](.*?);(.*?)[)]");
        List<SpatialFilter.Points> coordinatesPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Matcher matcher = pattern.matcher(points.get(i).trim());
            boolean matches = matcher.matches();
            if (matches)
                coordinatesPoints.add(SpatialFilter.Points.builder().latitude(Double.valueOf(matcher.group(1))).longitude(Double.valueOf(matcher.group(2))).build());
        }
        SpatialFilter.ByGeoPolygon byGeopolygon = SpatialFilter.ByGeoPolygon.builder().points(coordinatesPoints).build();
        spatialFilter.setByGeoPolygon(byGeopolygon);
    }

}
