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
        AntPathMatcher pathMatcher = new AntPathMatcher();
        final String path = request.path();
        final HttpMethod method = request.method();

        if ((configuration.getWhitelist() == null || configuration.getWhitelist().isEmpty()) &&
                        (configuration.getBlacklist() == null || configuration.getBlacklist().isEmpty())) {
            policyChain.doNext(request, response);
            return;
        }

        if (configuration.getWhitelist() != null && !configuration.getWhitelist().isEmpty()) {
            for(Resource resource : configuration.getWhitelist()) {
                if (resource.getPattern() != null && pathMatcher.match(resource.getPattern(), path)) {
                    if (resource.getMethods() == null || resource.getMethods().contains(method)) {
                        policyChain.doNext(request, response);
                        return;
                    }
                }
            }

            failWithForbidden(policyChain, path, method, request);
            return;
        }

        if (configuration.getBlacklist() != null && ! configuration.getBlacklist().isEmpty()) {
            for(Resource resource : configuration.getBlacklist()) {
                if (resource.getPattern() != null && pathMatcher.match(resource.getPattern(), path)) {
                    if (resource.getMethods() == null || resource.getMethods().contains(method)) {
                        failWithForbidden(policyChain, path, method, request);
                        return;
                    }
                }
            }

            policyChain.doNext(request, response);
        }
    }

    private void failWithForbidden(PolicyChain policyChain, String path, HttpMethod method, Request request) {
        request.metrics().setErrorKey(RESOURCE_FILTERING_FORBIDDEN);
        policyChain.failWith(
                PolicyResult.failure(
                        RESOURCE_FILTERING_FORBIDDEN,
                        HttpStatusCode.FORBIDDEN_403,
                        "You're not allowed to access this resource",
                        Maps.<String, Object>builder()
                                .put("path", path)
                                .put("method", method)
                                .build()));
    }
}
