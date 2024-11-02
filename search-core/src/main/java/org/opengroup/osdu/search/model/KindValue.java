package org.opengroup.osdu.search.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class KindValue {
    private String kind;
    private Object value;
}
