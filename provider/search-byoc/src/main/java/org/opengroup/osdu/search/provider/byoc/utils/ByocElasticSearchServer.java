package org.opengroup.osdu.search.provider.byoc.utils;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.search.provider.byoc.provider.persistence.ByocDatastoreFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.embeddedelasticsearch.IndexSettings;
import pl.allegro.tech.embeddedelasticsearch.PopularProperties;

import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

@Component
public class ByocElasticSearchServer implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${ELASTIC_SEARCH_VERSION}")
    private String elasticSearchVersion;

    @Value("${ELASTIC_SEARCH_PORT}")
    private int elasticSearchPort;

    @Value("${ELASTIC_SEARCH_CLUSTER_NAME}")
    private String clusterName;

    @Value("${ELASTIC_SEARCH_BYOC_INDEX}")
    private String byocIndex;

    @Inject
    private JaxRsDpsLog log;

    private EmbeddedElastic embeddedElastic = null;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {

        try{
            embeddedElastic = EmbeddedElastic.builder()
                    .withElasticVersion(elasticSearchVersion)
                    .withSetting(PopularProperties.TRANSPORT_TCP_PORT, elasticSearchPort)
                    .withSetting(PopularProperties.CLUSTER_NAME, clusterName)
                    .withStartTimeout(60, TimeUnit.SECONDS)
//                    .withPlugin("analysis-stempel")
                    .withIndex(byocIndex, IndexSettings.builder()
                            .withType("welldb", new ClassPathResource(
                                    "tenant-mappings.json").getInputStream())
                            .build())
                    .build()
                    .start();
            ByocDatastoreFactory.DATASTORE_CLIENTS.put("byoc", embeddedElastic);
            log.info(String.format("Embedded elastic search started on port : %s ", elasticSearchPort));

        }catch (Exception e){
            e.printStackTrace();
            log.error(String.format("Error starting embedded elastic search server %s", e), e);
        }

    }

}
