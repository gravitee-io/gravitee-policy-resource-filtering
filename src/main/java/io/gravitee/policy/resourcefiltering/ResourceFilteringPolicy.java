/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.resourcefiltering;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.policy.resourcefiltering.configuration.ResourceFilteringPolicyConfiguration;
import io.gravitee.policy.v3.resourcefiltering.ResourceFilteringPolicyV3;
import io.reactivex.rxjava3.core.Completable;
import org.springframework.util.AntPathMatcher;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourceFilteringPolicy extends ResourceFilteringPolicyV3 implements HttpPolicy {

    public ResourceFilteringPolicy(ResourceFilteringPolicyConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String id() {
        return "resource-filtering";
    }

    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return Completable.defer(() -> {
            final AntPathMatcher pathMatcher = new AntPathMatcher();
            String path = ctx.request().path();
            if (configuration.isNormalizeRequestPath()) {
                path = PathNormalizer.normalize(path, configuration.isDecodeEncodedSlash());
            }
            final String contextPath = ctx.request().contextPath();
            final io.gravitee.common.http.HttpMethod method = ctx.request().method();

            if (!match(true, contextPath, configuration.getWhitelist(), method, pathMatcher, path)) {
                if (methodMismatch(true, contextPath, configuration.getWhitelist(), method, pathMatcher, path)) {
                    return ctx.interruptWith(
                        new ExecutionFailure(HttpStatusCode.METHOD_NOT_ALLOWED_405)
                            .key(RESOURCE_FILTERING_METHOD_NOT_ALLOWED)
                            .message("Method not allowed while accessing this resource")
                    );
                }
                return ctx.interruptWith(
                    new ExecutionFailure(HttpStatusCode.FORBIDDEN_403)
                        .key(RESOURCE_FILTERING_FORBIDDEN)
                        .message("You're not allowed to access this resource")
                );
            } else if (match(false, contextPath, configuration.getBlacklist(), method, pathMatcher, path)) {
                if (methodMismatch(false, contextPath, configuration.getBlacklist(), method, pathMatcher, path)) {
                    return ctx.interruptWith(
                        new ExecutionFailure(HttpStatusCode.METHOD_NOT_ALLOWED_405)
                            .key(RESOURCE_FILTERING_METHOD_NOT_ALLOWED)
                            .message("Method not allowed while accessing this resource")
                    );
                }
                return ctx.interruptWith(
                    new ExecutionFailure(HttpStatusCode.FORBIDDEN_403)
                        .key(RESOURCE_FILTERING_FORBIDDEN)
                        .message("You're not allowed to access this resource")
                );
            }

            return Completable.complete();
        });
    }
}
