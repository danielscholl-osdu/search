package org.opengroup.osdu.request;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.core.common.model.search.SortQuery;

@Data
@NoArgsConstructor
public class CursorQuery {
    private String cursor;
    private String kind;
    private int limit;
    private String query;
    private List<String> returnedFields;
    private SortQuery sort;
    private Boolean queryAsOwner;
    private SpatialFilter spatialFilter;

    @Override
    public String toString() {
        return new com.google.gson.GsonBuilder().disableHtmlEscaping().create().toJson(this);
    }
}
