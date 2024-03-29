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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.gravitee.policy.api.PolicyConfiguration;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class ResourceFilteringPolicyConfiguration implements PolicyConfiguration {

    @JsonProperty("whitelist")
    private List<Resource> whitelist;

    @JsonProperty("blacklist")
    private List<Resource> blacklist;

    public List<Resource> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<Resource> whitelist) {
        this.whitelist = whitelist;
    }

    public List<Resource> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(List<Resource> blacklist) {
        this.blacklist = blacklist;
    }
}
