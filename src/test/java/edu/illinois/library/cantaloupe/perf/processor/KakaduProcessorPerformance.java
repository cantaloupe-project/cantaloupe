package edu.illinois.library.cantaloupe.perf.processor;

import java.util.concurrent.TimeUnit;

import edu.illinois.library.cantaloupe.processor.KakaduDemoProcessorTest;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import static edu.illinois.library.cantaloupe.test.PerformanceTestConstants.*;

/**
 * Executes benchmark to compare the speed of reading TIFF files.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = WARMUP_ITERATIONS,
        time = WARMUP_TIME,
        timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = MEASUREMENT_ITERATIONS,
        time = MEASUREMENT_TIME,
        timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M", "-Dcantaloupe.config=memory" })
public class KakaduProcessorPerformance extends KakaduDemoProcessorTest {

    @Benchmark
    @Override
    public void testReadImageInfoOnAllFixtures() throws Exception {
        super.testReadImageInfoOnAllFixtures();
    }

}
