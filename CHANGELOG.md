## [2.0.1](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/2.0.0...2.0.1) (2026-04-23)


### Bug Fixes

* **schema:** reorder booleans and gate decodeEncodedSlash on normalizeRequestPath ([ae53e4f](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/ae53e4ff801a36d96fee6533dbc38aaabad4bd83))

# [2.0.0](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.10.0...2.0.0) (2026-04-22)


### chore

* update project ([c0e7988](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/c0e798807410a1f595711ff8493fb3786138e2d1))


### Features

* add opt-in decoding of encoded slashes (%2F) during normalization ([3f217b1](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/3f217b1f2f97f70afd6867d3c9e1fc93e285b28d))
* add request path normalization to prevent bypass via encoding ([881277c](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/881277c83886fa925a9866127a9b583eea7ac93a))
* add V4 reactive support for Resource Filtering policy ([48e40b5](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/48e40b5bfba8300124ee6c71fbfe0bbc33086cf8))


### BREAKING CHANGES

* requires APIM 4.7.0 minimum and JDK 21

# [1.10.0](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.9.1...1.10.0) (2023-12-19)


### Features

* enable policy on REQUEST phase for message APIs ([90b0cca](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/90b0cca2e345a7c0413699e8d03ed12b1cf89e3b)), closes [gravitee-io/issues#9430](https://github.com/gravitee-io/issues/issues/9430)

## [1.9.1](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.9.0...1.9.1) (2023-07-20)


### Bug Fixes

* update policy description ([f735155](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/f7351556b5e7ab95e12bca7ba7d49720c10d79e2))

# [1.9.0](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.8.1...1.9.0) (2023-07-05)


### Features

* addition of the execution phase ([e943d77](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/e943d7738d02e535e529c0b170d99d1ad0068929))

## [1.8.1](https://github.com/gravitee-io/gravitee-policy-resource-filtering/compare/1.8.0...1.8.1) (2023-04-11)


### Bug Fixes

* clean schema-form to make them compatible with gio-form-json-schema component ([8abc436](https://github.com/gravitee-io/gravitee-policy-resource-filtering/commit/8abc436c2287f2f6e4be7bf41d3aadbff673a7bb))
