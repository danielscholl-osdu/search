package org.opengroup.osdu.request;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.core.common.model.search.SortQuery;

@Data
@NoArgsConstructor
public class Query {

    private String kind;
    private Integer offset;
    private Integer limit;
    private String query;
    private SortQuery sort;
    private Boolean queryAsOwner;
    private String aggregateBy;
    private List<String> returnedFields;
    private SpatialFilter spatialFilter;

    @Override
    public String toString() {
        return new com.google.gson.Gson().toJson(this);
    }
}
