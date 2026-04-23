You can use the `resource-filtering` policy to filter REST resources by path and HTTP method. It lets you restrict or allow access to specific resources so that, for example, a plan subscriber can only call read-only endpoints.

This policy is applicable to the following API types:

- v2 APIs
- v4 HTTP proxy APIs

The policy runs on the **request** phase. Requests that are not allowed are rejected with a `403 Forbidden` or `405 Method Not Allowed` before reaching the backend.

A typical usage is to allow access to all paths (`/**`) but in read-only mode (`GET` only).

> **Note:** Whitelist takes precedence over blacklist: a request that does not match the whitelist is rejected before the blacklist is even evaluated.
