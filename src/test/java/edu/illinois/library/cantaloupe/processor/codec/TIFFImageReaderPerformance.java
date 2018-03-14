package edu.illinois.library.cantaloupe.processor.codec;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
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
public class TIFFImageReaderPerformance extends TIFFImageReaderTest {

    @Setup
    public void setUp() throws Exception {
        super.setUp();
    }

    @TearDown
    public void tearDown() {
        super.tearDown();
    }

    @Benchmark
    @Override
    public void testGetCompressionWithUncompressedImage() throws Exception {
        super.testGetCompressionWithUncompressedImage();
    }

    @Benchmark
    @Override
    public void testGetMetadata() throws Exception {
        super.testGetMetadata();
    }

}
