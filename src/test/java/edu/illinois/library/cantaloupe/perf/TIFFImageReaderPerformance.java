package edu.illinois.library.cantaloupe.perf;

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

import edu.illinois.library.cantaloupe.processor.imageio.TIFFImageReaderTest;

/**
 * Executes benchmark to compare the speed of reading TIFF files.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M", "-Dcantaloupe.config=memory" })
public class TIFFImageReaderPerformance extends TIFFImageReaderTest {

    @Setup
    public void setUp() throws Exception {
        super.setUp();
    }

    @TearDown
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Benchmark
    public void testGetCompression() throws Exception {
        super.testGetCompression();
    }

    @Benchmark
    public void testGetMetadata() throws Exception {
        super.testGetMetadata();
    }

    @Benchmark
    public void testReadWithMonoResolutionImageAndNoScaleFactor() throws Exception {
        super.testReadWithMonoResolutionImageAndNoScaleFactor();
    }
}
