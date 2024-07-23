package org.opengroup.osdu.search.cache;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.cache.VmCache;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchClientCache implements ICache<String, ElasticsearchClient> {

  private final VmCache<String, ElasticsearchClient> vmCache;

  public ElasticsearchClientCache() {
    this.vmCache = new VmCache<>(20, 20);
  }

  @Override
  public void put(String partitionId, ElasticsearchClient client) {
    this.vmCache.put(partitionId, client);
  }

  @Override
  public ElasticsearchClient get(String partitionId) {
    return this.vmCache.get(partitionId);
  }

  @Override
  public void delete(String partitionId) {
    this.vmCache.delete(partitionId);
  }

  @Override
  public void clearAll() {
    this.vmCache.clearAll();
  }
}
