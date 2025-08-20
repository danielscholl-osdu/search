package org.opengroup.osdu.search.cache;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class ElasticsearchClientRemovalListener implements RemovalListener<String, ElasticsearchClient> {

    @Override
    public void onRemoval(RemovalNotification<String, ElasticsearchClient> notification) {
        ElasticsearchClient elasticSearchClient = notification.getValue();
        if (elasticSearchClient != null) {
            try {
                elasticSearchClient._transport().close();
            } catch (IOException e) {
                log.error("Error while closing transport on ElasticsearchClient {}", e.getMessage(), e);
            }
        }
    }

}
