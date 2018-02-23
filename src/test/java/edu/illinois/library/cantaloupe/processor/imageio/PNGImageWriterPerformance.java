package edu.illinois.library.cantaloupe.processor.imageio;

import java.util.concurrent.TimeUnit;

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

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = WARMUP_ITERATIONS,
        time = WARMUP_TIME)
@Measurement(iterations = MEASUREMENT_ITERATIONS,
        time = MEASUREMENT_TIME)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M", "-Dcantaloupe.config=memory" })
public class PNGImageWriterPerformance extends PNGImageWriterTest {

    @Benchmark
    @Override
    public void testWriteWithBufferedImage() throws Exception {
        super.testWriteWithBufferedImage();
    }

    @Benchmark
    @Override
    public void testWriteWithBufferedImageAndNativeMetadata() throws Exception {
        super.testWriteWithBufferedImageAndNativeMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithBufferedImageAndXMPMetadata() throws Exception {
        super.testWriteWithBufferedImageAndXMPMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithPlanarImage() throws Exception {
        super.testWriteWithPlanarImage();
    }

    @Benchmark
    @Override
    public void testWriteWithPlanarImageAndNativeMetadata() throws Exception {
        super.testWriteWithPlanarImageAndNativeMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithPlanarImageAndXMPMetadata() throws Exception {
        super.testWriteWithPlanarImageAndXMPMetadata();
    }

}
