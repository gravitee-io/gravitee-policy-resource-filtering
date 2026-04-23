
<!-- GENERATED CODE - DO NOT ALTER THIS OR THE FOLLOWING LINES -->
# Resource Filtering

[![Gravitee.io](https://img.shields.io/static/v1?label=Available%20at&message=Gravitee.io&color=1EC9D2)](https://download.gravitee.io/#graviteeio-apim/plugins/policies/gravitee-policy-resource-filtering/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/gravitee-io/gravitee-policy-resource-filtering/blob/master/LICENSE.txt)
[![Releases](https://img.shields.io/badge/semantic--release-conventional%20commits-e10079?logo=semantic-release)](https://github.com/gravitee-io/gravitee-policy-resource-filtering/releases)
[![CircleCI](https://circleci.com/gh/gravitee-io/gravitee-policy-resource-filtering.svg?style=svg)](https://circleci.com/gh/gravitee-io/gravitee-policy-resource-filtering)

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



## Phases
The `resource-filtering` policy can be applied to the following API types and flow phases.

### Compatible API types

* `PROXY`
* `MESSAGE`

### Supported flow phases:

* Request

## Compatibility matrix
Strikethrough text indicates that a version is deprecated.

| Plugin version| APIM |
| --- | ---  |
|2.x|4.7.x and upper |
|1.x|up to 4.6.x |


## Configuration options


#### 
| Name <br>`json name`  | Type <br>`constraint`  | Mandatory  | Default  | Description  |
|:----------------------|:-----------------------|:----------:|:---------|:-------------|
| Blacklist<br>`blacklist`| array|  | | <br/>See "Blacklist" section.|
| Decode encoded slashes (%2F)<br>`decodeEncodedSlash`| boolean|  | | Only applies when path normalization is enabled. When enabled, encoded slashes (%2F/%2f) are decoded to '/' during normalization. When disabled (default), they are preserved as literal %2F/%2f so that legitimate uses (e.g. identifiers containing a slash) are not altered. Enable for stricter security at the cost of rejecting paths that legitimately contain encoded slashes.|
| Normalize request path<br>`normalizeRequestPath`| boolean|  | | When enabled, the request path is normalized before evaluation (URL-decode, resolve dot segments, collapse double slashes). This prevents bypass via encoded paths.|
| Whitelist<br>`whitelist`| array|  | | <br/>See "Whitelist" section.|


#### Blacklist (Array)
| Name <br>`json name`  | Type <br>`constraint`  | Mandatory  | Description  |
|:----------------------|:-----------------------|:----------:|:-------------|
| Methods<br>`methods`| array (enum (string))<br>`unique`|  | HTTP Methods|
| Path pattern<br>`pattern`| string| ✅| Ant-style path patterns|


#### Whitelist (Array)
| Name <br>`json name`  | Type <br>`constraint`  | Mandatory  | Description  |
|:----------------------|:-----------------------|:----------:|:-------------|
| Methods<br>`methods`| array (enum (string))<br>`unique`|  | HTTP Methods|
| Path pattern<br>`pattern`| string| ✅| Ant-style path patterns|




## Examples

*Whitelist — read-only access to every resource*
```json
{
  "api": {
    "definitionVersion": "V4",
    "type": "PROXY",
    "name": "Resource Filtering example API",
    "flows": [
      {
        "name": "Common Flow",
        "enabled": true,
        "selectors": [
          {
            "type": "HTTP",
            "path": "/",
            "pathOperator": "STARTS_WITH"
          }
        ],
        "request": [
          {
            "name": "Resource Filtering",
            "enabled": true,
            "policy": "resource-filtering",
            "configuration":
              {
                  "whitelist": [
                      {
                          "pattern": "/**",
                          "methods": ["GET"]
                      }
                  ]
              }
          }
        ]
      }
    ]
  }
}

```
*Blacklist — deny every admin path*
```json
{
  "api": {
    "definitionVersion": "V4",
    "type": "PROXY",
    "name": "Resource Filtering example API",
    "flows": [
      {
        "name": "Common Flow",
        "enabled": true,
        "selectors": [
          {
            "type": "HTTP",
            "path": "/",
            "pathOperator": "STARTS_WITH"
          }
        ],
        "request": [
          {
            "name": "Resource Filtering",
            "enabled": true,
            "policy": "resource-filtering",
            "configuration":
              {
                  "blacklist": [
                      {
                          "pattern": "/**/admin/**"
                      }
                  ]
              }
          }
        ]
      }
    ]
  }
}

```
*Blacklist with path normalization (prevents %6e, /./, /../ bypass)*
```json
{
  "api": {
    "definitionVersion": "V4",
    "type": "PROXY",
    "name": "Resource Filtering example API",
    "flows": [
      {
        "name": "Common Flow",
        "enabled": true,
        "selectors": [
          {
            "type": "HTTP",
            "path": "/",
            "pathOperator": "STARTS_WITH"
          }
        ],
        "request": [
          {
            "name": "Resource Filtering",
            "enabled": true,
            "policy": "resource-filtering",
            "configuration":
              {
                  "blacklist": [
                      {
                          "pattern": "/**/admin/**"
                      }
                  ],
                  "normalizeRequestPath": true,
                  "decodeEncodedSlash": false
              }
          }
        ]
      }
    ]
  }
}

```
*Blacklist with encoded slash decoding (stricter, rejects %2F)*
```json
{
  "api": {
    "definitionVersion": "V4",
    "type": "PROXY",
    "name": "Resource Filtering example API",
    "flows": [
      {
        "name": "Common Flow",
        "enabled": true,
        "selectors": [
          {
            "type": "HTTP",
            "path": "/",
            "pathOperator": "STARTS_WITH"
          }
        ],
        "request": [
          {
            "name": "Resource Filtering",
            "enabled": true,
            "policy": "resource-filtering",
            "configuration":
              {
                  "blacklist": [
                      {
                          "pattern": "/**/admin/**"
                      }
                  ],
                  "normalizeRequestPath": true,
                  "decodeEncodedSlash": true
              }
          }
        ]
      }
    ]
  }
}

```


## Changelog

### [2.0.0](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.10.0...2.0.0) (2026-04-22)


##### chore

* update project ([c0e7988](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/c0e798807410a1f595711ff8493fb3786138e2d1))


##### Features

* add opt-in decoding of encoded slashes (%2F) during normalization ([3f217b1](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/3f217b1f2f97f70afd6867d3c9e1fc93e285b28d))
* add request path normalization to prevent bypass via encoding ([881277c](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/881277c83886fa925a9866127a9b583eea7ac93a))
* add V4 reactive support for Resource Filtering policy ([48e40b5](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/48e40b5bfba8300124ee6c71fbfe0bbc33086cf8))


##### BREAKING CHANGES

* requires APIM 4.7.0 minimum and JDK 21

### [1.10.0](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.9.1...1.10.0) (2023-12-19)


##### Features

* enable policy on REQUEST phase for message APIs ([90b0cca](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/90b0cca2e345a7c0413699e8d03ed12b1cf89e3b)), closes [gravitee-io/issues#9430](https://github.com/gravitee-io/issues/issues/9430)

#### [1.9.1](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.9.0...1.9.1) (2023-07-20)


##### Bug Fixes

* update policy description ([f735155](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/f7351556b5e7ab95e12bca7ba7d49720c10d79e2))

### [1.9.0](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.8.1...1.9.0) (2023-07-05)


##### Features

* addition of the execution phase ([e943d77](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/e943d7738d02e535e529c0b170d99d1ad0068929))

#### [1.8.1](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.8.0...1.8.1) (2023-04-11)


##### Bug Fixes

* clean schema-form to make them compatible with gio-form-json-schema component ([8abc436](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/8abc436c2287f2f6e4be7bf41d3aadbff673a7bb))

