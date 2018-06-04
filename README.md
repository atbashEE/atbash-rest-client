
# Atbash MicroProfile rest client 

MicroProfile rest client defines a type-safe approach for invoking RESTful services. 

This Atbash implementation has the following goals

. Independent implementation, not part of a larger framework or MP server implementation.
. Must be useable on any Java EE 7 and Java EE 8 compliant server
. Must be useable from Java 7

# Compliant, not certified.

Since we liked to have this Atbash implementation to be based on Java 7, The API code (Of Eclipse MicroProfile Rest client) is ported to Java 7 (as it is based on Java 8)

Due to the usage of some Java 8 features, the port to Java 7 is not identical and thus some small differences exists.

For example the programmatic retrieval of the Rest Client is done like this

    AbstractRestClientBuilder.newBuilder().build(HelloService.class);
    
But official way is

    RestClientBuilder.newBuilder().build(HelloService.class);


# Work in Progress

Only the basic features of the Rest Client 1.0 specification is implemented and ported. Supported is

. Calling JAX-RS endpoint.
. Using PathParam, QueryParam and HeaderParam
. Using methods GET, POST, PUT and DELETE
. Using CDI and programmatic lookup
. Using JSON payloads

Not supported for the moment

. Custom exception mapping


