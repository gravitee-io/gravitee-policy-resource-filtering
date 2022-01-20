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
package io.gravitee.policy.resourcefiltering.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourceFilteringPolicyConfigurationTest {

    @Test
    public void test_resourceFiltering01() throws IOException {
        ResourceFilteringPolicyConfiguration configuration = load(
            "/io/gravitee/policy/resourcefiltering/configuration/resourcefiltering01.json",
            ResourceFilteringPolicyConfiguration.class
        );

        Assert.assertNotNull(configuration);
        Assert.assertNull(configuration.getBlacklist());
        Assert.assertNotNull(configuration.getWhitelist());
    }

    @Test
    public void test_resourceFiltering02() throws IOException {
        ResourceFilteringPolicyConfiguration configuration = load(
            "/io/gravitee/policy/resourcefiltering/configuration/resourcefiltering02.json",
            ResourceFilteringPolicyConfiguration.class
        );

        Assert.assertNotNull(configuration);
        Assert.assertNotNull(configuration.getBlacklist());
        Assert.assertNotNull(configuration.getWhitelist());
    }

    @Test
    public void test_resourceFiltering03() throws IOException {
        ResourceFilteringPolicyConfiguration configuration = load(
            "/io/gravitee/policy/resourcefiltering/configuration/resourcefiltering03.json",
            ResourceFilteringPolicyConfiguration.class
        );

        List<Resource> whitelist = configuration.getWhitelist();
        Assert.assertNotNull(whitelist);
        Assert.assertFalse(whitelist.isEmpty());

        Resource resource = whitelist.iterator().next();
        Assert.assertNotNull(resource);
        Assert.assertNull(resource.getMethods());
    }

    private <T> T load(String resource, Class<T> type) throws IOException {
        URL jsonFile = this.getClass().getResource(resource);
        return new ObjectMapper().readValue(jsonFile, type);
    }
}
