package org.opengroup.osdu.search.util;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.info.ConnectedOuterServicesBuilder;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.info.ConnectedOuterService;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@ConditionalOnMissingBean(type = "ConnectedOuterServicesBuilder")
@Slf4j
@RequestScope
public class CloudConnectedOuterServicesBuilder implements ConnectedOuterServicesBuilder {

  private static final String NAME_PREFIX = "ElasticSearch-";
  private static final String REDIS_PREFIX = "Redis-";
  private static final String NOT_AVAILABLE = "N/A";

  private final List<RedisCache> redisCaches;
  private final ElasticClientHandler elasticClient;
  private final IElasticSettingService elasticSettingService;

  public CloudConnectedOuterServicesBuilder(
      List<RedisCache> redisCaches,
      ElasticClientHandler elasticClient,
      IElasticSettingService elasticSettingService) {
    this.redisCaches = redisCaches;
    this.elasticClient = elasticClient;
    this.elasticSettingService = elasticSettingService;
  }

  @Override
  public List<ConnectedOuterService> buildConnectedOuterServices() {
    return Stream.concat(redisCaches.stream().map(this::fetchRedisInfo),
        fetchElasticInfos().stream())
        .collect(Collectors.toList());
  }

  private ConnectedOuterService fetchRedisInfo(RedisCache cache) {
    String redisVersion = StringUtils.substringBetween(cache.info(), ":", "\r");
    return ConnectedOuterService.builder()
        .name(REDIS_PREFIX + StringUtils.substringAfterLast(cache.getClass().getName(), "."))
        .version(redisVersion)
        .build();
  }

  private List<ConnectedOuterService> fetchElasticInfos() {
    try {
      return elasticSettingService.getAllClustersSettings()
          .entrySet().stream()
          .map(entry -> fetchElasticInfo(entry.getKey(), entry.getValue()))
          .collect(Collectors.toList());
    } catch (AppException e) {
      log.error("Can't fetch cluster settings", e.getOriginalException());
      return Collections.singletonList(ConnectedOuterService.builder()
          .name(NAME_PREFIX + NOT_AVAILABLE)
          .version(NOT_AVAILABLE)
          .build());
    }
  }

  private ConnectedOuterService fetchElasticInfo(String partitionId, ClusterSettings settings) {
    try{
    ElasticsearchClient client = elasticClient.createRestClient(settings);

      return ConnectedOuterService.builder()
          .name(NAME_PREFIX + partitionId)
          .version(client.info().version().luceneVersion())
          .build();
    } catch (AppException e) {
      log.error("Can't create elastic client", e.getOriginalException());
      return ConnectedOuterService.builder()
          .name(NAME_PREFIX + partitionId)
          .version(NOT_AVAILABLE)
          .build();
    } catch (IOException | ElasticsearchException e) {
      log.error("Can't fetch elastic info.", e);
      return ConnectedOuterService.builder()
          .name(NAME_PREFIX + partitionId)
          .version(NOT_AVAILABLE)
          .build();
    }
  }
}