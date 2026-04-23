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

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.gravitee.policy.resourcefiltering.configuration.ResourceFilteringPolicyConfiguration;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * V4 integration tests for the semantic behavior of the Resource Filtering policy.
 *
 * <p>Each nested class deploys one API with a specific whitelist/blacklist configuration
 * and asserts the expected status code for representative requests.
 *
 * <p>Status code semantics:
 * <ul>
 *   <li><b>403 Forbidden</b> — the path itself is not accessible: whitelist miss, or blacklist match
 *       with no method restriction.</li>
 *   <li><b>405 Method Not Allowed</b> — the path is reachable but not with the requested method:
 *       whitelist path match with method mismatch, or blacklist path+method match (the caller could
 *       reach the same path with another method).</li>
 *   <li>When both lists are configured, the whitelist check runs first, then the blacklist.</li>
 * </ul>
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ResourceFilteringPolicySemanticsV4IntegrationTest {

    abstract static class BaseTest extends AbstractPolicyTest<ResourceFilteringPolicy, ResourceFilteringPolicyConfiguration> {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        void callAndExpect(HttpClient client, HttpMethod method, String path, int expectedStatus) {
            wiremock.stubFor(any(anyUrl()).willReturn(ok("response from backend")));
            client
                .rxRequest(method, path)
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).as("status code for %s %s", method, path).isEqualTo(expectedStatus);
                    return true;
                });
        }
    }

    // ============================================================
    // A. Whitelist only, no method restriction
    //    whitelist = [**/public/**]
    // ============================================================
    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/api-whitelist-only.json")
    class A_WhitelistOnly extends BaseTest {

        @Test
        void path_in_whitelist_is_allowed(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/public/foo", 200);
        }

        @Test
        void path_not_in_whitelist_is_forbidden(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/private/foo", 403);
        }

        @Test
        void any_method_is_allowed_on_whitelisted_path(HttpClient client) {
            callAndExpect(client, HttpMethod.POST, "/echo/public/foo", 200);
        }
    }

    // ============================================================
    // B. Whitelist only, with method GET
    //    whitelist = [**/api/** GET]
    // ============================================================
    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/api-whitelist-with-method.json")
    class B_WhitelistWithMethod extends BaseTest {

        @Test
        void allowed_path_and_method(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/api/foo", 200);
        }

        @Test
        void allowed_path_wrong_method_returns_405(HttpClient client) {
            callAndExpect(client, HttpMethod.POST, "/echo/api/foo", 405);
        }

        @Test
        void disallowed_path_returns_403(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/other/foo", 403);
        }

        @Test
        void disallowed_path_wrong_method_still_returns_403(HttpClient client) {
            // path is not whitelisted → 403 regardless of method
            callAndExpect(client, HttpMethod.POST, "/echo/other/foo", 403);
        }
    }

    // ============================================================
    // C. Blacklist only, no method restriction
    //    blacklist = [**/admin/**]
    // ============================================================
    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/api-blacklist-only.json")
    class C_BlacklistOnly extends BaseTest {

        @Test
        void path_in_blacklist_is_forbidden(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/admin/foo", 403);
        }

        @Test
        void path_not_in_blacklist_is_allowed(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/other/foo", 200);
        }

        @Test
        void blacklist_applies_to_any_method(HttpClient client) {
            callAndExpect(client, HttpMethod.POST, "/echo/admin/foo", 403);
        }
    }

    // ============================================================
    // D. Blacklist only, with method DELETE
    //    blacklist = [**/admin/** DELETE]
    // Path remains reachable with other methods → blacklisted method returns 405.
    // ============================================================
    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/api-blacklist-with-method.json")
    class D_BlacklistWithMethod extends BaseTest {

        @Test
        void blacklisted_method_on_blacklisted_path_returns_405(HttpClient client) {
            callAndExpect(client, HttpMethod.DELETE, "/echo/admin/foo", 405);
        }

        @Test
        void non_blacklisted_method_on_blacklisted_path_is_allowed(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/admin/foo", 200);
        }

        @Test
        void blacklisted_method_on_other_path_is_allowed(HttpClient client) {
            callAndExpect(client, HttpMethod.DELETE, "/echo/other/foo", 200);
        }
    }

    // ============================================================
    // E. Whitelist + Blacklist (no method)
    //    whitelist = [**/api/**], blacklist = [**/api/admin/**]
    // ============================================================
    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/api-whitelist-and-blacklist.json")
    class E_WhitelistAndBlacklist extends BaseTest {

        @Test
        void in_whitelist_and_not_in_blacklist_is_allowed(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/api/foo", 200);
        }

        @Test
        void in_both_lists_blacklist_wins(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/api/admin/x", 403);
        }

        @Test
        void not_in_whitelist_is_forbidden(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/other/foo", 403);
        }
    }

    // ============================================================
    // F. Whitelist + Blacklist with methods
    //    whitelist = [**/api/** GET,POST], blacklist = [**/api/admin/** DELETE]
    // ============================================================
    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/api-whitelist-and-blacklist-with-methods.json")
    class F_WhitelistAndBlacklistWithMethods extends BaseTest {

        @Test
        void allowed_method_on_api_is_allowed(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/api/foo", 200);
        }

        @Test
        void whitelist_method_mismatch_returns_405(HttpClient client) {
            callAndExpect(client, HttpMethod.DELETE, "/echo/api/foo", 405);
        }

        @Test
        void whitelist_allowed_and_blacklist_method_mismatch_is_allowed(HttpClient client) {
            // GET on /api/admin/x: whitelist passes (GET is allowed), blacklist only restricts DELETE → 200
            callAndExpect(client, HttpMethod.GET, "/echo/api/admin/x", 200);
        }

        @Test
        void delete_on_blacklisted_admin_is_caught_by_whitelist(HttpClient client) {
            // DELETE is not in whitelist methods → whitelist check fails first with 405
            callAndExpect(client, HttpMethod.DELETE, "/echo/api/admin/x", 405);
        }

        @Test
        void post_on_blacklisted_admin_is_allowed(HttpClient client) {
            // POST in whitelist, not in blacklist methods → 200
            callAndExpect(client, HttpMethod.POST, "/echo/api/admin/x", 200);
        }

        @Test
        void path_not_in_whitelist_is_forbidden(HttpClient client) {
            callAndExpect(client, HttpMethod.GET, "/echo/other/foo", 403);
        }
    }
}
