# Airtasker Rate Limiter

Thanks for given me the opportunity to enjoy working on this challenge. As the challenge does not specify
how the method should be exposed, what is the interface, and how the "return" value should be make, I tried
to build a first iteration for an extensible rate limiter.

The project is divided in submodules:

* core: it contains the logic of rate limiting, but it is quite generic, it does not impose any restrictions
  on the type of the request. This module contains one implementation of a generic RateLimiter based
  on SlideLog algorithm. Additionally, has a KeyBased rate limiter which uses a delegate pattern and maintains
  one `RateLimiter` for each key.
* jaxrs: Contains the integration classes for adapting the core rate limiters to the JAX-RS 2.1 specification.
  It contains a filter and also it provides a simple annotation that can be used in resource methods in order to
  provide simple rate limiting by resource method.
* jaxrs-sample-app: App with a sample resource URL `/sample` which is rate limited. It simulates a key based
  authentication (it actually just check if the header is in the request), and the rate limit is applied for 
  each different key.
  
## How to build

### Requirements

* JDK version 11 or later (it is tested with version 11)

### Build and test

Run the command (in linux or mac):

```
# ./gradlew check
```

### Run the sample application

Run the command (in linux or mac):

```
# ./gradlew run --args="-p 8080"
```

Change the number 8080 for a free port.

The application only have one endpoint `/sample` which is rate limited by default to 100 request per hour.

The rate limit is based on an api key which should be passed as a header `Api-Key`. In case the header is not present,
the response returns 403.

Example request with curl:
```
curl -v -H 'Api-Key: testing4' http://localhost:8081/sample
```

### JAXRS Module

This module contains a generic filter compatible with JAX-RS 2.1. It is tested with the Jerser which is the 
reference implementation.

Additionally includes a JAX-RS feature that adds rate limit to any resource method annotated with
`RateLimited`. When using with Jersey Autodiscovery enabled, the feature will be automatically registered,
otherwise the class `RateLimiterAnnotationFeature` needs to be registered in the JAX-RS framework.
