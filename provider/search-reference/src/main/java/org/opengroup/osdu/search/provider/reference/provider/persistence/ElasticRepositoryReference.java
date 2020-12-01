
package org.opengroup.osdu.search.provider.reference.provider.persistence;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.util.JSON.serialize;
import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import java.util.Objects;
import org.apache.http.HttpStatus;
import org.bson.Document;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.search.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticRepositoryReference implements IElasticRepository {

    @Value("${ELASTIC_DATASTORE_KIND}")
    private String ELASTIC_DATASTORE_KIND;

    @Value("${ELASTIC_DATASTORE_ID}")
    private String ELASTIC_DATASTORE_ID;

    @Value("${ELASTIC_HOST}")
    private String ELASTIC_HOST;
    @Value("${ELASTIC_PORT:443}")
    private String ELASTIC_PORT;
    @Value("${ELASTIC_USER_PASSWORD}")
    private String ELASTIC_USER_PASSWORD;

    @Autowired
    private MongoDdmsClient mongoClient;

    private static final Logger logger = LoggerFactory.getLogger(ElasticRepositoryReference.class);
    public static final String SEARCH_STORAGE = "SearchSettings";
    public static final String SEARCH_DATABASE = "local";

    @Override
    public ClusterSettings getElasticClusterSettings(TenantInfo tenantInfo) {

        if(tenantInfo == null) {
            throw  new AppException(HttpStatus.SC_NOT_FOUND, "TenantInfo is null", "");
        }

        String settingId = tenantInfo.getName().concat("-").concat(ELASTIC_DATASTORE_ID);

        MongoCollection<Document> collection = this.mongoClient
            .getMongoCollection(SEARCH_DATABASE, SEARCH_STORAGE);

        Document record = (Document) collection.find(eq("_id", settingId)).first();
        if (Objects.isNull(record)) {
            logger.warn(settingId + " credentials not found at database.");
            return new ClusterSettings(ELASTIC_HOST, Integer.parseInt(ELASTIC_PORT), ELASTIC_USER_PASSWORD, false, false);
        }

        ElasticSettingsDoc elasticSettingsDoc = new Gson()
            .fromJson(serialize(record), ElasticSettingsDoc.class);

        String host = elasticSettingsDoc.getSettingSchema().getHost();
        String portString = elasticSettingsDoc.getSettingSchema().getPort();
        String usernameAndPassword = elasticSettingsDoc.getSettingSchema().getUsernameAndPassword();

        Preconditions.checkNotNullOrEmpty(host, "host cannot be null");
        Preconditions.checkNotNullOrEmpty(portString, "port cannot be null");
        Preconditions.checkNotNullOrEmpty(usernameAndPassword, "configuration cannot be null");

        int port = Integer.parseInt(portString);

        return new ClusterSettings(host, port, usernameAndPassword,
            elasticSettingsDoc.getSettingSchema().isHttps(), elasticSettingsDoc.getSettingSchema().isHttps());
    }
}
