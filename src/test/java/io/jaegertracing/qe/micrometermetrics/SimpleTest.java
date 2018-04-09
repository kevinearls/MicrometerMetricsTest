package io.jaegertracing.qe.micrometermetrics;

import com.uber.jaeger.Tracer;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.micrometer.MicrometerMetricsFactory;
import com.uber.jaeger.reporters.LoggingReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ConstSampler;
import com.uber.jaeger.samplers.Sampler;
import com.uber.jaeger.samplers.SamplingStatus;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentracing.Span;
//import io.opentracing.Tracer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * What test cases?
 *
 * -- Get all metrics with no spans, just to be sure they're all there?
 * -- Generate spans to create which ones?
 */
public class SimpleTest {
    private static final Map<String, String> envs = System.getenv();
    private static final String TEST_SERVICE_NAME = envs.getOrDefault("TEST_SERVICE_NAME", "metrics");

    private static final Logger logger = LoggerFactory.getLogger(SimpleTest.class.getName());
    @Test
    public void simpleTest() throws InterruptedException {
        MicrometerMetricsFactory metricsReporter = new MicrometerMetricsFactory();
        Metrics metrics = new Metrics(metricsReporter);

        Reporter loggingReporter = new LoggingReporter();
        Sampler constantSampler = new ConstSampler(true);

        Tracer tracer = new com.uber.jaeger.Tracer.Builder(TEST_SERVICE_NAME)
                .withReporter(loggingReporter)  // DO we need this?
                .withSampler(constantSampler)
                .withMetrics(metrics)
                .build();

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        io.micrometer.core.instrument.Metrics.addRegistry(registry);

        Thread.sleep(1000);

        createSomeSpans(tracer);

        SamplingStatus samplingStatus = constantSampler.sample("test", 0L);
        logger.info("SAMPLING STATUS: " + samplingStatus.toString());

        tracer.close();
        Thread.sleep(2000);  // TODO shorten FLUSH interval?

        List<Meter> meters = new ArrayList<>(registry.getMeters());
        meters.sort((m1, m2) -> m1.getId().getName().compareTo(m2.getId().getName()));

        // Display all metrics
        logger.info("--------------------------------------------------------------------------------------");
        for (Meter m: meters) {
            List<Measurement> measurements = new ArrayList<>();
            m.measure().forEach(measurements::add);
            logger.info("\t" + m.getId() + " " + m.measure().toString());
        }


        logger.info("--------------------------------------------------------------------------------------");
        // Display non-zero metrics
        for (Meter m: meters) {
            List<Measurement> measurements = new ArrayList<>();
            m.measure().forEach(measurements::add);
            if (measurements.get(0).getValue() > 0.0) {
                logger.info("\t" + m.getId() + " " + m.measure().toString());
            }
            //logger.info("\tSize " + measurements.size() + " " + measurements.get(0).getValue());
        }

        logger.info("--------------------------------------------------------------------------------------");

        double dd = registry.get("jaeger:finished_spans")
                .counter()
                .count();

        //System.out.println("Huh? " + dd):
        logger.info("Huh " + dd);

        double started = registry.get("jaeger:started_spans")
                .tag("sampled", "n")
                .counter()
                .count();
        System.out.println("Stared with n " + started);
    }

    private void createSomeSpans(Tracer tracer) {
        for (int i=0; i < 10; i++) {
            Span fred = tracer.buildSpan("fred")
                    .withTag("foo", "bar")
                    .start();

            if (i % 3 == 0) {
                fred.finish();
            }
        }
    }
}
