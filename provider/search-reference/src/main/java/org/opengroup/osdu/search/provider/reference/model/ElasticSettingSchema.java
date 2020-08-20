package org.opengroup.osdu.search.provider.reference.model;

import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElasticSettingSchema {

    @NotEmpty
    private String host;

    @NotEmpty
    private String port;

    @NotEmpty
    private String usernameAndPassword;

    @NotEmpty
    private boolean isHttps;  

}
