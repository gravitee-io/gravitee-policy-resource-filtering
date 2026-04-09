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
package io.gravitee.policy.v3.resourcefiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.resourcefiltering.configuration.Resource;
import io.gravitee.policy.resourcefiltering.configuration.ResourceFilteringPolicyConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ResourceFilteringPolicyV3Test {

    private ResourceFilteringPolicyV3 resourceFilteringPolicy;

    @Mock
    private ResourceFilteringPolicyConfiguration resourceFilteringPolicyConfiguration;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    protected PolicyChain policyChain;

    @BeforeEach
    void init() {
        resourceFilteringPolicy = new ResourceFilteringPolicyV3(resourceFilteringPolicyConfiguration);
    }

    @Test
    void testOnRequest_noFiltering() {
        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(null);
        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(null);

        when(request.path()).thenReturn("/path");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_emptyFiltering() {
        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(new ArrayList<>());
        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(new ArrayList<>());

        when(request.path()).thenReturn("/path");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_singleWhitelistFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/**");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_singleBlacklistFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/**");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void testOnRequest_singleBlacklistFiltering_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void testOnRequest_singleWhitelistWithMethodFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");
        when(request.method()).thenReturn(HttpMethod.POST);

        ArgumentCaptor<PolicyResult> policyResultCaptor = ArgumentCaptor.forClass(PolicyResult.class);

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(policyResultCaptor.capture());

        final PolicyResult value = policyResultCaptor.getValue();
        assertNotNull(value);
        assertEquals(HttpStatusCode.METHOD_NOT_ALLOWED_405, value.statusCode());
        assertEquals("RESOURCE_FILTERING_METHOD_NOT_ALLOWED", value.key());
    }

    @Test
    void testOnRequest_singleWhitelistWithMethodFiltering2() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");
        when(request.method()).thenReturn(HttpMethod.GET);

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_singleBlacklistWithMethodFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");
        when(request.method()).thenReturn(HttpMethod.POST);

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_singleBlacklistWithMethodFiltering2() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");
        when(request.method()).thenReturn(HttpMethod.GET);

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void testOnRequest_antPatternFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void testOnRequest_antPatternFiltering_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void testOnRequest_antPatternFiltering2() {
        Resource resource = new Resource();
        resource.setPattern("/products/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_antPatternFiltering2_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_antPatternFiltering3() {
        Resource resource = new Resource();
        resource.setPattern("/products/**/prices");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices");
        when(request.contextPath()).thenReturn("/products/123456/store_12/prices/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void testOnRequest_antPatternFiltering3_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**/prices");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void testOnRequest_antPatternFiltering4() {
        Resource resource = new Resource();
        resource.setPattern("/products/**/prices");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/123456/store_12/prices/toto/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_antPatternFiltering4_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**/prices");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_antPatternFiltering5() {
        Resource resource = new Resource();
        resource.setPattern("/products/**/prices");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/123456/store_12/prices/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void testOnRequest_antPatternFiltering5_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**/prices");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void testOnRequest_antPatternFiltering6() {
        Resource resource = new Resource();
        resource.setPattern("/products/**/prices/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/123456/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_antPatternFiltering6_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**/prices/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_antPatternFiltering7_withContextPath_multipleResources_ok_ko() {
        Resource resource1 = new Resource();
        resource1.setPattern("/**/prices/*");
        Resource resource2 = new Resource();
        resource2.setPattern("/**/media/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Arrays.asList(resource1, resource2));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void testOnRequest_antPatternFiltering8_withContextPath_multipleResources_ko_ok() {
        Resource resource1 = new Resource();
        resource1.setPattern("/**/prices/*");
        Resource resource2 = new Resource();
        resource2.setPattern("/**/media/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Arrays.asList(resource2, resource1));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    // --- Path normalization tests ---

    @Test
    void should_block_blacklisted_path_with_url_encoding() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(resourceFilteringPolicyConfiguration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/admi%6e/test");
        when(request.contextPath()).thenReturn("/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void should_block_blacklisted_path_with_double_slash() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(resourceFilteringPolicyConfiguration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/admin//test");
        when(request.contextPath()).thenReturn("/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void should_block_blacklisted_path_with_dot_segment() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(resourceFilteringPolicyConfiguration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/admin/./test");
        when(request.contextPath()).thenReturn("/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void should_block_blacklisted_path_with_parent_traversal() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(resourceFilteringPolicyConfiguration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/admin/../admin/test");
        when(request.contextPath()).thenReturn("/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    void should_allow_encoded_whitelist_path_when_normalized() {
        Resource resource = new Resource();
        resource.setPattern("/public/**");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(Collections.singletonList(resource));
        when(resourceFilteringPolicyConfiguration.isNormalizeRequestPath()).thenReturn(true);
        when(request.path()).thenReturn("/publi%63/data");
        when(request.contextPath()).thenReturn("/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    void should_not_normalize_when_disabled() {
        Resource resource = new Resource();
        resource.setPattern("/admin/**");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(Collections.singletonList(resource));
        when(resourceFilteringPolicyConfiguration.isNormalizeRequestPath()).thenReturn(false);
        when(request.path()).thenReturn("/admi%6e/test");
        when(request.contextPath()).thenReturn("/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }
}
