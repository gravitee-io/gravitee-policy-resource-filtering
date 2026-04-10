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
     * Normalize a URL path, preserving encoded slashes ({@code %2F}/{@code %2f}) as literal
     * characters. Equivalent to {@link #normalize(String, boolean)} with {@code decodeEncodedSlash=false}.
     */
    public static String normalize(String path) {
        return normalize(path, false);
    }

    /**
     * Normalize a URL path:
     * <ul>
     *   <li>URL-decode percent-encoded characters (%6e → n)</li>
     *   <li>Collapse multiple slashes (// → /)</li>
     *   <li>Remove dot segments (/./ and /../) per RFC 3986 §5.2.4</li>
     * </ul>
     *
     * @param path the request path to normalize
     * @param decodeEncodedSlash when {@code true}, encoded slashes ({@code %2F}/{@code %2f}) are
     *                           decoded to {@code /} and then collapsed and resolved like any
     *                           other slash. When {@code false}, they are preserved as literal
     *                           {@code %2F}/{@code %2f} in the output so that legitimate uses
     *                           (e.g. identifiers containing a slash) are not altered.
     */
    public static String normalize(String path, boolean decodeEncodedSlash) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        String toDecode = decodeEncodedSlash ? path : protectEncodedSlashes(path);
        String decoded = URLDecoder.decode(toDecode, StandardCharsets.UTF_8);
        decoded = decoded.replaceAll("/+", "/");
        decoded = removeDotSegments(decoded);

        return decoded;
    }

    /**
     * Double-encode {@code %2F}/{@code %2f} to {@code %252F}/{@code %252f} so that a subsequent
     * {@link URLDecoder#decode} call produces the literal characters {@code %2F}/{@code %2f}
     * instead of {@code /}.
     */
    private static String protectEncodedSlashes(String path) {
        return path.replace("%2F", "%252F").replace("%2f", "%252f");
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
