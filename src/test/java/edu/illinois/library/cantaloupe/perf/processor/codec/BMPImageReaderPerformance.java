package edu.illinois.library.cantaloupe.perf.processor.codec;

import java.util.concurrent.TimeUnit;

import edu.illinois.library.cantaloupe.processor.codec.BMPImageReaderTest;
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
public class BMPImageReaderPerformance extends BMPImageReaderTest {

    @Setup
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @TearDown
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Benchmark
    @Override
    public void testGetCompression() throws Exception {
        super.testGetCompression();
    }

    @Benchmark
    @Override
    public void testGetMetadata() throws Exception {
        super.testGetMetadata();
    }

    @Benchmark
    @Override
    public void testGetNumImages() throws Exception {
        super.testGetNumImages();
    }

    @Benchmark
    @Override
    public void testGetSize() throws Exception {
        super.testGetSize();
    }

    @Benchmark
    @Override
    public void testRead() throws Exception {
        super.testRead();
    }

    @Benchmark
    @Override
    public void testReadWithArguments() throws Exception {
        super.testReadWithArguments();
    }

    @Benchmark
    @Override
    public void testReadRendered() throws Exception {
        super.testReadRendered();
    }

    @Benchmark
    @Override
    public void testReadRenderedWithArguments() throws Exception {
        super.testReadRenderedWithArguments();
    }

}
