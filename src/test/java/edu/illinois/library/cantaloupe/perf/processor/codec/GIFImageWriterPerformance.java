package edu.illinois.library.cantaloupe.perf.processor.codec;

import java.util.concurrent.TimeUnit;

import edu.illinois.library.cantaloupe.processor.codec.GIFImageWriterTest;
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

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = WARMUP_ITERATIONS,
        time = WARMUP_TIME)
@Measurement(iterations = MEASUREMENT_ITERATIONS,
        time = MEASUREMENT_TIME)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M", "-Dcantaloupe.config=memory" })
public class GIFImageWriterPerformance extends GIFImageWriterTest {

    @Setup
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @TearDown
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Benchmark
    @Override
    public void testWriteWithBufferedImage() throws Exception {
        super.testWriteWithBufferedImage();
    }

    @Benchmark
    @Override
    public void testWriteWithBufferedImageAndMetadata() throws Exception {
        super.testWriteWithBufferedImageAndMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithPlanarImage() throws Exception {
        super.testWriteWithPlanarImage();
    }

    @Benchmark
    @Override
    public void testWriteWithPlanarImageAndMetadata() throws Exception {
        super.testWriteWithPlanarImageAndMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithSequence() throws Exception {
        super.testWriteWithSequence();
    }

}
