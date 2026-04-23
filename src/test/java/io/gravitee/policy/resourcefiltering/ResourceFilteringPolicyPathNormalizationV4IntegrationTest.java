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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * V4 integration tests covering the path normalization feature (PEN-42 / APIM-12931 / APIM-13563).
 *
 * <p>API context-path is {@code /echo}, blacklist pattern is {@code **&#47;admin/test/**}.
 * Requests whose path normalizes to {@code /echo/admin/test/...} must be blocked with 403.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ResourceFilteringPolicyPathNormalizationV4IntegrationTest {

    abstract static class BaseTest extends AbstractPolicyTest<ResourceFilteringPolicy, ResourceFilteringPolicyConfiguration> {

        @Override
        public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
            entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
        }

        @Override
        public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
            endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
        }

        void callAndExpect(HttpClient client, String path, int expectedStatus) {
            client
                .rxRequest(HttpMethod.GET, path)
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(expectedStatus);
                    return true;
                });
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/api-normalize-disabled.json")
    class WhenNormalizationIsDisabled extends BaseTest {

        @ParameterizedTest
        @ValueSource(
            strings = {
                "/echo/admi%6e/test/foo/bar",
                "/echo/admin/./test/foo/bar",
                "/echo/admin/../test/foo/bar",
                "/echo/admin/../superadmin/test/foo/bar",
            }
        )
        void should_not_block_malformed_paths(String path, HttpClient client) {
            wiremock.stubFor(any(anyUrl()).willReturn(ok("response from backend")));

            callAndExpect(client, path, 200);
        }

        @ParameterizedTest
        @ValueSource(strings = { "/echo/admin//test/foo/bar" })
        void should_block_double_slash_due_to_ant_matcher(String path, HttpClient client) {
            // AntPathMatcher tokenizes paths with ignoreEmptyTokens=true, so "//" is always
            // collapsed before the blacklist pattern is evaluated — regardless of the
            // normalizeRequestPath flag.
            wiremock.stubFor(any(anyUrl()).willReturn(ok("response from backend")));

            callAndExpect(client, path, 403);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/api-normalize-enabled.json")
    class WhenNormalizationIsEnabled extends BaseTest {

        @ParameterizedTest
        @ValueSource(strings = { "/echo/admi%6e/test/foo/bar", "/echo/admin//test/foo/bar", "/echo/admin/./test/foo/bar" })
        void should_block_paths_that_normalize_into_blacklist(String path, HttpClient client) {
            wiremock.stubFor(any(anyUrl()).willReturn(ok("response from backend")));

            callAndExpect(client, path, 403);
        }

        @ParameterizedTest
        @ValueSource(strings = { "/echo/admin/../test/foo/bar", "/echo/admin/../superadmin/test/foo/bar" })
        void should_not_block_paths_that_normalize_out_of_blacklist(String path, HttpClient client) {
            wiremock.stubFor(any(anyUrl()).willReturn(ok("response from backend")));

            callAndExpect(client, path, 200);
        }

        @ParameterizedTest
        @ValueSource(strings = { "/echo/admin%2Ftest/foo/bar", "/echo/admin%2ftest/foo/bar" })
        void should_not_decode_encoded_slash_by_default(String path, HttpClient client) {
            // decodeEncodedSlash=false: %2F / %2f are preserved as literal characters, so the
            // path keeps "admin%2Ftest" as a single segment and does not match "**/admin/test/**".
            wiremock.stubFor(any(anyUrl()).willReturn(ok("response from backend")));

            callAndExpect(client, path, 200);
        }
    }

    @Nested
    @GatewayTest
    @DeployApi("/apis/v4/api-decode-encoded-slash.json")
    class WhenDecodeEncodedSlashIsEnabled extends BaseTest {

        @ParameterizedTest
        @ValueSource(strings = { "/echo/admin%2Ftest/foo/bar", "/echo/admin%2ftest/foo/bar" })
        void should_block_when_encoded_slash_is_decoded(String path, HttpClient client) {
            // decodeEncodedSlash=true: %2F / %2f are decoded to "/", so "admin%2Ftest" becomes
            // "admin/test" and matches the blacklist pattern.
            wiremock.stubFor(any(anyUrl()).willReturn(ok("response from backend")));

            callAndExpect(client, path, 403);
        }
    }
}
