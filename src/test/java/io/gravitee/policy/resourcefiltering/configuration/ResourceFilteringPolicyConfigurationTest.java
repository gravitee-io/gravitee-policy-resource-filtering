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
package io.gravitee.policy.resourcefiltering.configuration;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class ResourceFilteringPolicyConfigurationTest {

    @Test
    void test_resourceFiltering01() throws IOException {
        ResourceFilteringPolicyConfiguration configuration = load(
            "/io/gravitee/policy/resourcefiltering/configuration/resourcefiltering01.json",
            ResourceFilteringPolicyConfiguration.class
        );

        assertNotNull(configuration);
        assertNull(configuration.getBlacklist());
        assertNotNull(configuration.getWhitelist());
    }

    @Test
    void test_resourceFiltering02() throws IOException {
        ResourceFilteringPolicyConfiguration configuration = load(
            "/io/gravitee/policy/resourcefiltering/configuration/resourcefiltering02.json",
            ResourceFilteringPolicyConfiguration.class
        );

        assertNotNull(configuration);
        assertNotNull(configuration.getBlacklist());
        assertNotNull(configuration.getWhitelist());
    }

    @Test
    void test_resourceFiltering03() throws IOException {
        ResourceFilteringPolicyConfiguration configuration = load(
            "/io/gravitee/policy/resourcefiltering/configuration/resourcefiltering03.json",
            ResourceFilteringPolicyConfiguration.class
        );

        List<Resource> whitelist = configuration.getWhitelist();
        assertNotNull(whitelist);
        assertFalse(whitelist.isEmpty());

        Resource resource = whitelist.iterator().next();
        assertNotNull(resource);
        assertNull(resource.getMethods());
    }

    @Test
    void normalizeRequestPath_should_default_to_false_when_absent() {
        ResourceFilteringPolicyConfiguration configuration = new ResourceFilteringPolicyConfiguration();

        assertFalse(configuration.isNormalizeRequestPath());
    }

    @Test
    void normalizeRequestPath_should_return_true_when_explicitly_enabled() {
        ResourceFilteringPolicyConfiguration configuration = new ResourceFilteringPolicyConfiguration();
        configuration.setNormalizeRequestPath(true);

        assertTrue(configuration.isNormalizeRequestPath());
    }

    @Test
    void normalizeRequestPath_should_return_false_when_explicitly_disabled() {
        ResourceFilteringPolicyConfiguration configuration = new ResourceFilteringPolicyConfiguration();
        configuration.setNormalizeRequestPath(false);

        assertFalse(configuration.isNormalizeRequestPath());
    }

    @Test
    void decodeEncodedSlash_should_default_to_false_when_absent() {
        ResourceFilteringPolicyConfiguration configuration = new ResourceFilteringPolicyConfiguration();

        assertFalse(configuration.isDecodeEncodedSlash());
    }

    @Test
    void decodeEncodedSlash_should_return_true_when_explicitly_enabled() {
        ResourceFilteringPolicyConfiguration configuration = new ResourceFilteringPolicyConfiguration();
        configuration.setDecodeEncodedSlash(true);

        assertTrue(configuration.isDecodeEncodedSlash());
    }

    @Test
    void decodeEncodedSlash_should_return_false_when_explicitly_disabled() {
        ResourceFilteringPolicyConfiguration configuration = new ResourceFilteringPolicyConfiguration();
        configuration.setDecodeEncodedSlash(false);

        assertFalse(configuration.isDecodeEncodedSlash());
    }

    private <T> T load(String resource, Class<T> type) throws IOException {
        URL jsonFile = this.getClass().getResource(resource);
        return new ObjectMapper().readValue(jsonFile, type);
    }
}
