package edu.illinois.library.cantaloupe.processor.codec;

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
public class JPEGImageWriterPerformance extends JPEGImageWriterTest {

    @Benchmark
    @Override
    public void testWriteWithBufferedImage() throws Exception {
        super.testWriteWithBufferedImage();
    }

    @Benchmark
    @Override
    public void testWriteWithBufferedImageAndExifMetadata() throws Exception {
        super.testWriteWithBufferedImageAndExifMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithBufferedImageAndIptcMetadata() throws Exception {
        super.testWriteWithBufferedImageAndIptcMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithBufferedImageAndXmpMetadata() throws Exception {
        super.testWriteWithBufferedImageAndXmpMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithPlanarImage() throws Exception {
        super.testWriteWithPlanarImage();
    }

    @Benchmark
    @Override
    public void testWriteWithPlanarImageAndExifMetadata() throws Exception {
        super.testWriteWithPlanarImageAndExifMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithPlanarImageAndIptcMetadata() throws Exception {
        super.testWriteWithPlanarImageAndIptcMetadata();
    }

    @Benchmark
    @Override
    public void testWriteWithPlanarImageAndXmpMetadata() throws Exception {
        super.testWriteWithPlanarImageAndXmpMetadata();
    }

}
