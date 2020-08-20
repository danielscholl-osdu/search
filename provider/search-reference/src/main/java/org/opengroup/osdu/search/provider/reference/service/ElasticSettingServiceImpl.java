package org.opengroup.osdu.search.provider.reference.service;

import javax.inject.Inject;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.indexer.IElasticSettingService;
import org.opengroup.osdu.core.common.model.search.ClusterSettings;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.multitenancy.ITenantInfoService;
import org.opengroup.osdu.core.common.provider.interfaces.IElasticRepository;
import org.opengroup.osdu.core.common.search.Config;
import org.opengroup.osdu.search.provider.reference.cache.ElasticCredentialsCache;
import org.springframework.stereotype.Service;

@Service
public class ElasticSettingServiceImpl implements IElasticSettingService {

  @Inject
  private javax.inject.Provider<ITenantInfoService> tenantInfoServiceProvider;

  @Inject
  private IElasticRepository elasticRepository;

  @Inject
  private ElasticCredentialsCache elasticCredentialCache;

  @Inject
  private JaxRsDpsLog log;

  @Override
  public ClusterSettings getElasticClusterInformation() {

    TenantInfo tenantInfo = this.tenantInfoServiceProvider.get().getTenantInfo();

    String cacheKey = String.format("%s-%s", Config.getDeployedServiceId(), tenantInfo.getName());
    ClusterSettings clusterInfo = this.elasticCredentialCache.get(cacheKey);
    if (clusterInfo != null) {
      return clusterInfo;
    }
    log.warning(String.format("elastic-credential cache missed for tenant: %s", tenantInfo.getName()));

    clusterInfo = this.elasticRepository.getElasticClusterSettings(tenantInfo);
    if (clusterInfo == null) {
      throw new AppException(HttpStatus.SC_NOT_FOUND, "Tenant not found", "No information about the given tenant was found");
    }

    this.elasticCredentialCache.put(cacheKey, clusterInfo);
    return clusterInfo;
  }
}
