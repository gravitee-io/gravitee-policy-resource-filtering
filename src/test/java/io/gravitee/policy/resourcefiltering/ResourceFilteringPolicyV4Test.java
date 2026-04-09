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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.policy.resourcefiltering.configuration.Resource;
import io.gravitee.policy.resourcefiltering.configuration.ResourceFilteringPolicyConfiguration;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceFilteringPolicyV4Test {

    @Mock
    private ResourceFilteringPolicyConfiguration configuration;

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private HttpPlainRequest request;

    private ResourceFilteringPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new ResourceFilteringPolicy(configuration);
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.interruptWith(any())).thenReturn(Completable.error(new InterruptedException()));
    }

    // --- No filtering ---

    @Test
    void should_continue_when_no_filtering() {
        when(configuration.getWhitelist()).thenReturn(null);
        when(configuration.getBlacklist()).thenReturn(null);
        when(request.path()).thenReturn("/path");

        policy.onRequest(ctx).test().assertComplete();
    }

    @Test
    void should_continue_when_empty_filtering() {
        when(configuration.getWhitelist()).thenReturn(new ArrayList<>());
        when(configuration.getBlacklist()).thenReturn(new ArrayList<>());
        when(request.path()).thenReturn("/path");

        policy.onRequest(ctx).test().assertComplete();
    }

    // --- Whitelist ---

    @Test
    void should_allow_when_path_matches_whitelist() {
        Resource resource = new Resource();
        resource.setPattern("/**");

        when(configuration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123");
        when(request.contextPath()).thenReturn("/products/123/");

        policy.onRequest(ctx).test().assertComplete();
    }

    @Test
    void should_deny_when_path_does_not_match_whitelist() {
        Resource resource = new Resource();
        resource.setPattern("/allowed/**");

        when(configuration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/forbidden/data");
        when(request.contextPath()).thenReturn("/");

        policy.onRequest(ctx).test().assertError(InterruptedException.class);

        verify(ctx).interruptWith(argThat(failure -> failure.statusCode() == HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    void should_deny_with_405_when_method_mismatch_on_whitelist() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(configuration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123");
        when(request.contextPath()).thenReturn("/products/123/");
        when(request.method()).thenReturn(HttpMethod.POST);

        policy.onRequest(ctx).test().assertError(InterruptedException.class);

        verify(ctx).interruptWith(argThat(failure -> failure.statusCode() == HttpStatusCode.METHOD_NOT_ALLOWED_405));
    }

    @Test
    void should_allow_when_method_matches_whitelist() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(configuration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123");
        when(request.contextPath()).thenReturn("/products/123/");
        when(request.method()).thenReturn(HttpMethod.GET);

        policy.onRequest(ctx).test().assertComplete();
    }

    // --- Blacklist ---

    @Test
    void should_deny_when_path_matches_blacklist() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/admin/secret");
        when(request.contextPath()).thenReturn("/");

        policy.onRequest(ctx).test().assertError(InterruptedException.class);

        verify(ctx).interruptWith(argThat(failure -> failure.statusCode() == HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    void should_allow_when_path_does_not_match_blacklist() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/public/data");
        when(request.contextPath()).thenReturn("/");

        policy.onRequest(ctx).test().assertComplete();
    }

    @Test
    void should_deny_with_405_when_method_matches_blacklist() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.DELETE));

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123");
        when(request.contextPath()).thenReturn("/");
        when(request.method()).thenReturn(HttpMethod.DELETE);

        policy.onRequest(ctx).test().assertError(InterruptedException.class);

        verify(ctx).interruptWith(argThat(failure -> failure.statusCode() == HttpStatusCode.METHOD_NOT_ALLOWED_405));
    }

    @Test
    void should_allow_when_method_does_not_match_blacklist() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123");
        when(request.contextPath()).thenReturn("/");
        when(request.method()).thenReturn(HttpMethod.POST);

        policy.onRequest(ctx).test().assertComplete();
    }

    // --- Ant patterns with context path ---

    @Test
    void should_deny_with_context_path_pattern() {
        Resource resource = new Resource();
        resource.setPattern("/**/prices");

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123/store/prices");
        when(request.contextPath()).thenReturn("/products/");

        policy.onRequest(ctx).test().assertError(InterruptedException.class);

        verify(ctx).interruptWith(argThat(failure -> failure.statusCode() == HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    void should_allow_with_context_path_single_level_wildcard() {
        Resource resource = new Resource();
        resource.setPattern("/*");

        when(configuration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123");
        when(request.contextPath()).thenReturn("/products/");

        policy.onRequest(ctx).test().assertComplete();
    }

    @Test
    void should_allow_whitelist_with_multiple_resources_first_matches() {
        Resource resource1 = new Resource();
        resource1.setPattern("/**/prices/*");
        Resource resource2 = new Resource();
        resource2.setPattern("/**/media/*");

        when(configuration.getWhitelist()).thenReturn(Arrays.asList(resource1, resource2));
        when(request.path()).thenReturn("/products/123/store/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        policy.onRequest(ctx).test().assertComplete();
    }

    @Test
    void should_allow_whitelist_with_multiple_resources_second_matches() {
        Resource resource1 = new Resource();
        resource1.setPattern("/**/prices/*");
        Resource resource2 = new Resource();
        resource2.setPattern("/**/media/*");

        when(configuration.getWhitelist()).thenReturn(Arrays.asList(resource2, resource1));
        when(request.path()).thenReturn("/products/123/store/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        policy.onRequest(ctx).test().assertComplete();
    }

    // --- Path normalization tests ---

    @Test
    void should_block_blacklisted_path_with_url_encoding() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(configuration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/admi%6e/test");
        when(request.contextPath()).thenReturn("/");

        policy.onRequest(ctx).test().assertError(InterruptedException.class);

        verify(ctx).interruptWith(argThat(failure -> failure.statusCode() == HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    void should_block_blacklisted_path_with_double_slash() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(configuration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/admin//test");
        when(request.contextPath()).thenReturn("/");

        policy.onRequest(ctx).test().assertError(InterruptedException.class);

        verify(ctx).interruptWith(argThat(failure -> failure.statusCode() == HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    void should_block_blacklisted_path_with_dot_segment() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(configuration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/admin/./test");
        when(request.contextPath()).thenReturn("/");

        policy.onRequest(ctx).test().assertError(InterruptedException.class);

        verify(ctx).interruptWith(argThat(failure -> failure.statusCode() == HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    void should_block_blacklisted_path_with_parent_traversal() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(configuration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/admin/../admin/test");
        when(request.contextPath()).thenReturn("/");

        policy.onRequest(ctx).test().assertError(InterruptedException.class);

        verify(ctx).interruptWith(argThat(failure -> failure.statusCode() == HttpStatusCode.FORBIDDEN_403));
    }

    @Test
    void should_allow_encoded_whitelist_path_when_normalized() {
        Resource resource = new Resource();
        resource.setPattern("/public/**");

        when(configuration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(configuration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/publi%63/data");
        when(request.contextPath()).thenReturn("/");

        policy.onRequest(ctx).test().assertComplete();
    }

    @Test
    void should_not_normalize_when_disabled() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(configuration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(configuration.isNormalizeRequestPath()).thenReturn(false);
        when(request.path()).thenReturn("/admi%6e/test");
        when(request.contextPath()).thenReturn("/");

        policy.onRequest(ctx).test().assertComplete();
    }
}
