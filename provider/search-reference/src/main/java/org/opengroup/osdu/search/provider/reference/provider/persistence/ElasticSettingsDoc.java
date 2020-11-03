package org.opengroup.osdu.search.provider.reference.provider.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opengroup.osdu.search.provider.reference.model.ElasticSettingSchema;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ElasticSettingsDoc {
	
	public static final String DB_NAME =  "SearchSettings";   //collection name
	
    @Id
    private String _id;
    private String _rev;
    private ElasticSettingSchema settingSchema;
    
	public void setId(String id) {
		this._id = id;		
	}
	
}
