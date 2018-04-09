# Test of Jaeger client micrometer metrics

## To run

Start Jaeger

+ export JAEGER_SERVICE_NAME="jaeger-client-java-tester"
+ export JAEGER_REPORTER_LOG_SPANS="true"
+ export JAEGER_SAMPLER_TYPE="const"
+ export JAEGER_SAMPLER_PARAM="1"

`mvn clean install vertx:run`

`curl localhost:8080` To produce spans
`curl localhost:8081` To get metrics


TODO how to start prometheus....

## Background
See `https://github.com/jaegertracing/jaeger-client-java/pull/356` and 