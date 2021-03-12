package edu.illinois.library.cantaloupe.perf.processor.codec.png;

import java.util.concurrent.TimeUnit;

import edu.illinois.library.cantaloupe.processor.codec.png.PNGImageReaderTest;
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
public class PNGImageReaderPerformance extends PNGImageReaderTest {

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
    public void testRead1() throws Exception {
        super.testRead1();
    }

    @Benchmark
    @Override
    public void testRead2() throws Exception {
        super.testRead2();
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
