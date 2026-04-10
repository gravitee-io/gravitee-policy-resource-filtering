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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PathNormalizerTest {

    @Test
    void should_return_null_for_null_path() {
        assertNull(PathNormalizer.normalize(null));
    }

    @Test
    void should_return_empty_for_empty_path() {
        assertEquals("", PathNormalizer.normalize(""));
    }

    @Test
    void should_not_change_clean_path() {
        assertEquals("/admin/test", PathNormalizer.normalize("/admin/test"));
    }

    @Test
    void should_decode_url_encoded_characters() {
        assertEquals("/admin/test", PathNormalizer.normalize("/admi%6e/test"));
    }

    @Test
    void should_decode_multiple_encoded_characters() {
        assertEquals("/admin/test.php", PathNormalizer.normalize("/%61dmin/%74est.php"));
    }

    @Test
    void should_collapse_double_slashes() {
        assertEquals("/admin/test", PathNormalizer.normalize("/admin//test"));
    }

    @Test
    void should_collapse_triple_slashes() {
        assertEquals("/admin/test", PathNormalizer.normalize("/admin///test"));
    }

    @Test
    void should_resolve_dot_segments() {
        assertEquals("/admin/test", PathNormalizer.normalize("/admin/./test"));
    }

    @Test
    void should_resolve_parent_traversal() {
        assertEquals("/admin/test", PathNormalizer.normalize("/admin/../admin/test"));
    }

    @Test
    void should_resolve_parent_traversal_at_root() {
        assertEquals("/secret/test", PathNormalizer.normalize("/api/../secret/test"));
    }

    @Test
    void should_handle_combined_attack_vectors() {
        assertEquals("/admin/test", PathNormalizer.normalize("/admi%6e/./test"));
    }

    @Test
    void should_handle_encoding_and_double_slash() {
        assertEquals("/admin/test", PathNormalizer.normalize("/admi%6e//test"));
    }

    @Test
    void should_preserve_trailing_slash() {
        assertEquals("/admin/test/", PathNormalizer.normalize("/admin/test/"));
    }

    @Test
    void should_handle_root_path() {
        assertEquals("/", PathNormalizer.normalize("/"));
    }

    @Test
    void should_preserve_encoded_slash_by_default() {
        assertEquals("/api/users/alice%2Fbob", PathNormalizer.normalize("/api/users/alice%2Fbob"));
    }

    @Test
    void should_preserve_lowercase_encoded_slash_by_default() {
        assertEquals("/api/users/alice%2fbob", PathNormalizer.normalize("/api/users/alice%2fbob"));
    }

    @Test
    void should_preserve_encoded_slash_and_still_decode_other_encodings() {
        assertEquals("/admin/alice%2Fbob", PathNormalizer.normalize("/admi%6e/alice%2Fbob"));
    }

    @Test
    void should_not_resolve_dot_segments_hidden_behind_encoded_slash_by_default() {
        // %2F stays literal, so the "/.." is not a real path segment and is not resolved.
        assertEquals("/api/users/alice%2F..%2Fadmin", PathNormalizer.normalize("/api/users/alice%2F..%2Fadmin"));
    }

    @Test
    void should_decode_encoded_slash_when_enabled() {
        assertEquals("/api/users/alice/bob", PathNormalizer.normalize("/api/users/alice%2Fbob", true));
    }

    @Test
    void should_decode_lowercase_encoded_slash_when_enabled() {
        assertEquals("/api/users/alice/bob", PathNormalizer.normalize("/api/users/alice%2fbob", true));
    }

    @Test
    void should_resolve_dot_segments_hidden_behind_encoded_slash_when_enabled() {
        // %2F becomes /, dot segments are then resolved, revealing the bypass attempt:
        // /api/users/alice/../admin -> /api/users/admin
        assertEquals("/api/users/admin", PathNormalizer.normalize("/api/users/alice%2F..%2Fadmin", true));
    }
}
