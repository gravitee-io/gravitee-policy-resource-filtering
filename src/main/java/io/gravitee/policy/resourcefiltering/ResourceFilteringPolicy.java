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

import io.gravitee.common.http.HttpStatusCode;
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
        String path = request.path();

        if ((configuration.getWhitelist() == null || configuration.getWhitelist().isEmpty()) &&
                        (configuration.getBlacklist() == null || configuration.getBlacklist().isEmpty())) {
            policyChain.doNext(request, response);
            return;
        }

        if (configuration.getWhitelist() != null && !configuration.getWhitelist().isEmpty()) {
            for(Resource resource : configuration.getWhitelist()) {
                if (pathMatcher.match(resource.getPattern(), path)) {
                    if (resource.getMethods() == null || resource.getMethods().contains(request.method())) {
                        policyChain.doNext(request, response);
                        return;
                    }
                }
            }

            policyChain.failWith(PolicyResult.failure(
                    HttpStatusCode.FORBIDDEN_403,
                    "You're not allowed to access this resource"));
            return;
        }

        if (configuration.getBlacklist() != null && ! configuration.getBlacklist().isEmpty()) {
            for(Resource resource : configuration.getBlacklist()) {
                if (pathMatcher.match(resource.getPattern(), path)) {
                    if (resource.getMethods() == null || resource.getMethods().contains(request.method())) {
                        policyChain.failWith(PolicyResult.failure(
                                HttpStatusCode.FORBIDDEN_403,
                                "You're not allowed to access this resource"));
                        return;
                    }
                }
            }

            policyChain.doNext(request, response);
            return;
        }
    }
}
