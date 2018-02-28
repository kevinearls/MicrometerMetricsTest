package io.jaegertracing.qe.micrometermetrics;

import com.uber.jaeger.Configuration;
//import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.micrometer.MicrometerMetricsFactory;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class MainVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    public static void main(String[] args) {
        System.setProperty("JAEGER_SERVICE_NAME", "jaeger-client-java-tester");
        System.setProperty("JAEGER_REPORTER_LOG_SPANS", "true");
        System.setProperty("JAEGER_SAMPLER_TYPE", "const");
        System.setProperty("JAEGER_SAMPLER_PARAM", "1");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start() {
        MicrometerMetricsFactory metricsReporter = new MicrometerMetricsFactory();
        Configuration configuration = Configuration.fromEnv();
        //Metrics metrics = new
        Tracer tracer = configuration
                .getTracerBuilder()
                .withMetrics(new com.uber.jaeger.metrics.Metrics(metricsReporter))
                .build();

        GlobalTracer.register(tracer);
        logger.warn("Registered tracer: " + GlobalTracer.get().toString());

        vertx.createHttpServer()
                .requestHandler(req -> {
                    Span span = GlobalTracer.get().buildSpan("new-request").start();
                    req.response().end("Hello from Vert.x at " + Instant.now().toString() + "!");
                    span.finish();
                })
                .listen(8080);

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Metrics.addRegistry(registry);

        vertx.createHttpServer()
                .requestHandler(req -> req.response().end(registry.scrape()))
                .listen(8081);
    }

}