// Copyright © Amazon
// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.provider.aws.entitlements;

import com.lambdaworks.redis.RedisException;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsFactory;
import org.opengroup.osdu.core.common.entitlements.IEntitlementsService;
import org.opengroup.osdu.core.common.http.HeadersUtil;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.entitlements.AuthorizationResponse;
import org.opengroup.osdu.core.common.model.entitlements.EntitlementsException;
import org.opengroup.osdu.core.common.model.entitlements.GroupInfo;
import org.opengroup.osdu.core.common.model.entitlements.Groups;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.IAuthorizationService;
import org.opengroup.osdu.core.common.util.Crc32c;
import org.opengroup.osdu.search.provider.aws.cache.GroupCache;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Primary // overrides class in core common
@Lazy
public class AWSAuthorizationServiceImpl implements IAuthorizationService {
	private static final String TENANT_GROUP_FORMAT = "@%s";
	private static final String ERROR_REASON = "Access denied";
	private static final String ERROR_MSG = "The user is not authorized to perform this action";

	@Inject
	private IEntitlementsFactory factory;

	@Inject
	@Lazy
	private JaxRsDpsLog jaxRsDpsLog;

	@Inject
	private GroupCache<String, Groups> cache;

	@Override
	public AuthorizationResponse authorizeAny(DpsHeaders headers, String... roles) {
		AuthorizationResponse authorizationResponse = null;
		try {
			authorizationResponse = authorizeAny(headers, getGroups(headers), roles);
		} catch (Exception e) {
			handleEntitlementsException(e, headers);
		}
		return authorizationResponse;
	}

	protected static String getGroupCacheKey(DpsHeaders headers) {
		String key = String.format("entitlement-groups:%s:%s", headers.getPartitionIdWithFallbackToAccountId(),
				headers.getAuthorization());
		return Crc32c.hashToBase64EncodedString(key);
	}

	public Groups getGroups(DpsHeaders headers) {
		String cacheKey = this.getGroupCacheKey(headers);

		Groups groups = null;
		try {
			groups = this.cache.get(cacheKey);
		} catch (RedisException ex) {
			this.jaxRsDpsLog.error(String.format("Error getting key %s from redis: %s", cacheKey, ex.getMessage()), ex);
		}

		if (groups == null) {
			IEntitlementsService service = this.factory.create(headers);
			try {
				groups = service.getGroups();
				this.cache.put(cacheKey, groups);
				this.jaxRsDpsLog.debug("Entitlements cache miss");

			} catch (EntitlementsException e) {
				HttpResponse response = e.getHttpResponse();
				this.jaxRsDpsLog.error(String.format("Error requesting entitlements service %s", response));
				throw new AppException(e.getHttpResponse().getResponseCode(), ERROR_REASON, ERROR_MSG, e);
			} catch (RedisException ex) {
				this.jaxRsDpsLog.error(String.format("Error putting key %s into redis: %s", cacheKey, ex.getMessage()), ex);
			}
		}
		return groups;
	}

	@Override
	public AuthorizationResponse authorizeAny(String tenantName, DpsHeaders headers, String... roles) {
		AuthorizationResponse authorizationResponse = null;
		try {
			Groups groups = getGroups(headers);
			List<GroupInfo> allGroups = new ArrayList<>(groups.getGroups());
			groups.setGroups(groups.getGroups().stream().filter(groupInfo -> groupInfo.getEmail()
					.contains(String.format(TENANT_GROUP_FORMAT, tenantName))).collect(Collectors.toList()));

			authorizationResponse = authorizeAny(headers, groups, roles);
			groups.setGroups(allGroups);
		} catch (Exception e) {
			handleEntitlementsException(e, headers);
		}
		return authorizationResponse;
	}

	private void handleEntitlementsException(Exception e, DpsHeaders headers) {
		throw new AppException(500, "Access denied", "The user is not authorized to perform this action", HeadersUtil.toLogMsg(headers, null), e);
	}

	private AuthorizationResponse authorizeAny(DpsHeaders headers, Groups groups, String... roles) {
		String userEmail = null;
		List<String> logMessages = new ArrayList<>();
		Long curTimeStamp = System.currentTimeMillis();
		Long latency = System.currentTimeMillis() - curTimeStamp;

		logMessages.add(String.format("entitlements-api latency: %s", latency));
		logMessages.add(String.format("groups: %s", getEmailFromGroups(groups)));
		if (groups != null) {
			userEmail = groups.getMemberEmail();
			if (groups.any(roles)) {
				return AuthorizationResponse.builder().user(userEmail).groups(groups).build();
			}
		}
		jaxRsDpsLog.info(String.join(" | ", logMessages));
		jaxRsDpsLog.info(HeadersUtil.toLogMsg(headers, userEmail));
		throw AppException.createUnauthorized("required search service roles are missing for user");
	}

	private String getEmailFromGroups(Groups groups) {
		if (groups == null) return "";
		return groups.getGroups().stream().map(GroupInfo::getEmail).collect(Collectors.joining(" | "));
	}
}
