## Overview
You can use the `resource-filtering` policy to filter REST resources by path and HTTP method. It lets you restrict or allow access to specific resources so that, for example, a plan subscriber can only call read-only endpoints.

This policy is applicable to the following API types:

- v2 APIs
- v4 HTTP proxy APIs

The policy runs on the **request** phase. Requests that are not allowed are rejected with a `403 Forbidden` or `405 Method Not Allowed` before reaching the backend.

A typical usage is to allow access to all paths (`/**`) but in read-only mode (`GET` only).

> **Note:** Whitelist takes precedence over blacklist: a request that does not match the whitelist is rejected before the blacklist is even evaluated.



## Usage
## Whitelist and blacklist

Both lists accept an array of rules, where a rule is defined as:

| Property  | Required | Description                                                                                         | Type            | Default           |
|-----------|:--------:|-----------------------------------------------------------------------------------------------------|-----------------|-------------------|
| `pattern` | ✅        | An [Ant-style path pattern](#ant-style-path-pattern) matched against the request path.              | string          | -                 |
| `methods` |          | HTTP methods the rule applies to. When omitted, the rule applies to every method.                   | array of string | all HTTP methods  |

### Semantics

| Configuration                          | Request                                    | Result                  |
|----------------------------------------|--------------------------------------------|-------------------------|
| Path in whitelist (no method)          | any                                        | 200 (allowed)           |
| Path in whitelist, method match        | same method                                | 200 (allowed)           |
| Path in whitelist, method mismatch     | other method                               | **405** Method Not Allowed |
| Path not in whitelist                  | any                                        | **403** Forbidden       |
| Path in blacklist (no method)          | any                                        | **403** Forbidden       |
| Path in blacklist, method match        | same method                                | **405** Method Not Allowed |
| Path in blacklist, method mismatch     | other method                               | 200 (allowed)           |

> When both a whitelist and a blacklist are configured, the whitelist is evaluated first. A request that passes the whitelist is then checked against the blacklist.

<a id="ant-style-path-pattern"></a>

### Ant-style path patterns

Patterns follow the [Apache Ant](http://ant.apache.org/) convention:

* `?` matches one character
* `*` matches zero or more characters within a path segment
* `**` matches zero or more directories in a path

---

## Path normalization

Whitelist and blacklist patterns can otherwise be bypassed by paths that differ only through encoding tricks — for example `%6e` instead of `n`, `/./`, `/../`, or `//`. Two opt-in flags make the policy resilient to this.

### `normalizeRequestPath` (default `false`)

When enabled, the request path is normalized before whitelist and blacklist evaluation:

* URL-decode percent-encoded characters (for example `%6e` → `n`).
* Collapse repeated slashes (`//` → `/`).
* Resolve dot segments per [RFC 3986 §5.2.4](https://www.rfc-editor.org/rfc/rfc3986#section-5.2.4) (`/./` is removed, `/../` removes the previous segment).

### `decodeEncodedSlash` (default `false`, only effective when `normalizeRequestPath` is `true`)

Controls how encoded slashes (`%2F` / `%2f`) are handled during normalization:

* `false`: encoded slashes are preserved as literal `%2F` / `%2f`. Useful when identifiers in your URL legitimately contain a slash.
* `true`: encoded slashes are decoded to `/`, then collapsed and resolved like any other slash. Stricter for security, at the cost of rejecting paths that legitimately contain encoded slashes.

### Implicit behavior on `//`

Even when `normalizeRequestPath` is `false`, patterns are matched using Spring's `AntPathMatcher`, which tokenizes paths with `ignoreEmptyTokens=true`. As a consequence, `//` is always treated as `/` during pattern matching — independent of the normalization flag. This only affects duplicate slashes, not other encoding tricks.




## Errors
These templates are defined at the API level, in the "Entrypoint" section for v4 APIs, or in "Response Templates" for v2 APIs.
The error keys sent by this policy are as follows:

| Key |
| ---  |
| RESOURCE_FILTERING_FORBIDDEN |
| RESOURCE_FILTERING_METHOD_NOT_ALLOWED |


