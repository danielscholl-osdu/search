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

package org.opengroup.osdu.search.smart.middleware;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.search.smart.models.AttributeCollection;
import org.opengroup.osdu.search.smart.models.Kinds;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

public class SyncFilterValuesServiceImpl implements SyncFilterValuesService {

	@Inject
	private Kinds kinds;

	@Inject
	private AttributeCollection attributes;

	@Override
	public void updateCache() {
		try {
			kinds.cacheSync();
			attributes.cacheSync();
		} catch (IOException | URISyntaxException ex) {
			throw new AppException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Failed to sync kinds into the cache.",ex.getMessage());
		}
	}
}
