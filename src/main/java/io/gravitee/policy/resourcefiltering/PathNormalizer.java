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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Normalizes URL paths to prevent bypass via encoding tricks.
 */
public final class PathNormalizer {

    private PathNormalizer() {}

    /**
     * Normalize a URL path:
     * <ul>
     *   <li>URL-decode percent-encoded characters (%6e → n)</li>
     *   <li>Collapse multiple slashes (// → /)</li>
     *   <li>Remove dot segments (/./ and /../) per RFC 3986 §5.2.4</li>
     * </ul>
     */
    public static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
        decoded = decoded.replaceAll("/+", "/");
        decoded = removeDotSegments(decoded);

        return decoded;
    }

    /**
     * Remove dot segments from a path per RFC 3986 §5.2.4.
     */
    private static String removeDotSegments(String path) {
        StringBuilder output = new StringBuilder();
        int i = 0;
        while (i < path.length()) {
            if (path.startsWith("../", i)) {
                i += 3;
            } else if (path.startsWith("./", i)) {
                i += 2;
            } else if (path.startsWith("/./", i)) {
                i += 2;
            } else if (i + 2 == path.length() && path.startsWith("/.", i)) {
                output.append('/');
                i += 2;
            } else if (path.startsWith("/../", i)) {
                i += 3;
                removeLastSegment(output);
            } else if (i + 3 == path.length() && path.startsWith("/..", i)) {
                removeLastSegment(output);
                output.append('/');
                i += 3;
            } else if ((i == path.length() - 1 && path.charAt(i) == '.') || (i == path.length() - 2 && path.startsWith("..", i))) {
                break;
            } else {
                int segStart = i;
                if (path.charAt(i) == '/') {
                    i++;
                }
                int nextSlash = path.indexOf('/', i);
                if (nextSlash == -1) {
                    nextSlash = path.length();
                }
                output.append(path, segStart, nextSlash);
                i = nextSlash;
            }
        }

        return output.length() == 0 ? "/" : output.toString();
    }

    private static void removeLastSegment(StringBuilder output) {
        int lastSlash = output.lastIndexOf("/");
        if (lastSlash >= 0) {
            output.setLength(lastSlash);
        }
    }
}
