/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.resourcefiltering;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.Maps;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.resourcefiltering.configuration.Resource;
import io.gravitee.policy.resourcefiltering.configuration.ResourceFilteringPolicyConfiguration;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourceFilteringPolicy {

    /**
     * The associated configuration to this Resource Filtering Policy
     */
    private ResourceFilteringPolicyConfiguration configuration;

    private static final String RESOURCE_FILTERING_FORBIDDEN = "RESOURCE_FILTERING_FORBIDDEN";

    /**
     * Create a new Resource Filtering Policy instance based on its associated configuration
     *
     * @param configuration the associated configuration to the new  Resource Filtering Policy instance
     */
    public ResourceFilteringPolicy(ResourceFilteringPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnRequest
    public void onRequest(Request request, Response response, PolicyChain policyChain) {
        final AntPathMatcher pathMatcher = new AntPathMatcher();

        if (!validate(true, request.contextPath(), configuration.getWhitelist(), request.method(), pathMatcher, request.path())
                || !validate(false, request.contextPath(), configuration.getBlacklist(), request.method(), pathMatcher, request.path())) {

            policyChain.failWith(
                    PolicyResult.failure(
                            RESOURCE_FILTERING_FORBIDDEN,
                            HttpStatusCode.FORBIDDEN_403,
                            "You're not allowed to access this resource",
                            Maps.<String, Object>builder()
                                    .put("path", request.path())
                                    .put("method", request.method())
                                    .build()));
            return ;
        }

        policyChain.doNext(request, response);
    }

    private boolean validate(boolean whitelist, String contextPath, List<Resource> resources, HttpMethod method,
                             PathMatcher pathMatcher, String path) {
        if (resources == null || resources.isEmpty()) {
            return true;
        }

        for (Resource resource : resources) {
            if (((resource.getMethods() == null || resource.getMethods().contains(method)) &&
                    (resource.getPattern() == null ||
                            pathMatcher.match(resource.getPattern(), path) ||
                            pathMatcher.match(contextPath + resource.getPattern(), path))) != whitelist ) {
                return false;
            }
        }

        return true;
    }
}
