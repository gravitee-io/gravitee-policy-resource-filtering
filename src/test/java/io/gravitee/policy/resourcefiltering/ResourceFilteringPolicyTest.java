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
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.resourcefiltering.configuration.Resource;
import io.gravitee.policy.resourcefiltering.configuration.ResourceFilteringPolicyConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceFilteringPolicyTest {

    private ResourceFilteringPolicy resourceFilteringPolicy;

    @Mock
    private ResourceFilteringPolicyConfiguration resourceFilteringPolicyConfiguration;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    protected PolicyChain policyChain;

    @Before
    public void init() {
        resourceFilteringPolicy = new ResourceFilteringPolicy(resourceFilteringPolicyConfiguration);
    }

    @Test
    public void testOnRequest_noFiltering() {
        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(null);
        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(null);

        when(request.path()).thenReturn("/path");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_emptyFiltering() {
        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(new ArrayList<>());
        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(new ArrayList<>());

        when(request.path()).thenReturn("/path");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_singleWhitelistFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/**");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_singleBlacklistFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/**");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    public void testOnRequest_singleBlacklistFiltering_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    public void testOnRequest_singleWhitelistWithMethodFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");
        when(request.method()).thenReturn(HttpMethod.POST);

        ArgumentCaptor<PolicyResult> poilcyResultCaptor = ArgumentCaptor.forClass(PolicyResult.class);

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(poilcyResultCaptor.capture());

        final PolicyResult value = poilcyResultCaptor.getValue();
        assertNotNull(value);
        assertEquals(HttpStatusCode.METHOD_NOT_ALLOWED_405, value.statusCode());
        assertEquals("RESOURCE_FILTERING_METHOD_NOT_ALLOWED", value.key());
    }

    @Test
    public void testOnRequest_singleWhitelistWithMethodFiltering2() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");
        when(request.method()).thenReturn(HttpMethod.GET);

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_singleBlacklistWithMethodFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");
        when(request.method()).thenReturn(HttpMethod.POST);

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request ,response);
    }

    @Test
    public void testOnRequest_singleBlacklistWithMethodFiltering2() {
        Resource resource = new Resource();
        resource.setPattern("/**");
        resource.setMethods(Collections.singletonList(HttpMethod.GET));

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");
        when(request.method()).thenReturn(HttpMethod.GET);

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    public void testOnRequest_antPatternFiltering() {
        Resource resource = new Resource();
        resource.setPattern("/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/123456/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    public void testOnRequest_antPatternFiltering_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    public void testOnRequest_antPatternFiltering2() {
        Resource resource = new Resource();
        resource.setPattern("/products/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_antPatternFiltering2_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_antPatternFiltering3() {
        Resource resource = new Resource();
        resource.setPattern("/products/**/prices");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices");
        when(request.contextPath()).thenReturn("/products/123456/store_12/prices/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    public void testOnRequest_antPatternFiltering3_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**/prices");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    public void testOnRequest_antPatternFiltering4() {
        Resource resource = new Resource();
        resource.setPattern("/products/**/prices");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/123456/store_12/prices/toto/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_antPatternFiltering4_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**/prices");

        when(resourceFilteringPolicyConfiguration.getBlacklist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_antPatternFiltering5() {
        Resource resource = new Resource();
        resource.setPattern("/products/**/prices");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/123456/store_12/prices/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    public void testOnRequest_antPatternFiltering5_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**/prices");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).failWith(any(PolicyResult.class));
    }

    @Test
    public void testOnRequest_antPatternFiltering6() {
        Resource resource = new Resource();
        resource.setPattern("/products/**/prices/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/123456/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_antPatternFiltering6_withContextPath() {
        Resource resource = new Resource();
        resource.setPattern("/**/prices/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Collections.singletonList(resource));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_antPatternFiltering7_withContextPath_multipleResources_ok_ko() {
        Resource resource1 = new Resource();
        resource1.setPattern("/**/prices/*");
        Resource resource2 = new Resource();
        resource2.setPattern("/**/media/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Arrays.asList(resource1, resource2));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }

    @Test
    public void testOnRequest_antPatternFiltering8_withContextPath_multipleResources_ko_ok() {
        Resource resource1 = new Resource();
        resource1.setPattern("/**/prices/*");
        Resource resource2 = new Resource();
        resource2.setPattern("/**/media/*");

        when(resourceFilteringPolicyConfiguration.getWhitelist()).thenReturn(
                Arrays.asList(resource2, resource1));
        when(request.path()).thenReturn("/products/123456/store_12/prices/toto");
        when(request.contextPath()).thenReturn("/products/");

        resourceFilteringPolicy.onRequest(request, response, policyChain);

        verify(policyChain).doNext(request, response);
    }
}
