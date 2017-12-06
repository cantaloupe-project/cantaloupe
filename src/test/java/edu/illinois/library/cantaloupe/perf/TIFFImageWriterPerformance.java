package edu.illinois.library.cantaloupe.perf;

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

import edu.illinois.library.cantaloupe.processor.imageio.TIFFImageWriterTest;

/**
 * Executes benchmark to compare the speed of writing TIFF files.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = { "-server", "-Xms128M", "-Xmx128M", "-Dcantaloupe.config=memory" })
public class TIFFImageWriterPerformance extends TIFFImageWriterTest {

    @Benchmark
    public void testWriteWithBufferedImage() throws Exception {
        super.testWriteWithBufferedImage();
    }

    @Benchmark
    public void testWriteWithBufferedImageAndEXIFMetadata() throws Exception {
        super.testWriteWithBufferedImageAndEXIFMetadata();
    }

    @Benchmark
    public void testWriteWithBufferedImageAndIPTCMetadata() throws Exception {
        super.testWriteWithBufferedImageAndIPTCMetadata();
    }

    @Benchmark
    public void testWriteWithBufferedImageAndXMPMetadata() throws Exception {
        super.testWriteWithBufferedImageAndXMPMetadata();
    }

    @Benchmark
    public void testWriteWithPlanarImage() throws Exception {
        super.testWriteWithPlanarImage();
    }

    @Benchmark
    public void testWriteWithPlanarImageAndEXIFMetadata() throws Exception {
        super.testWriteWithPlanarImageAndEXIFMetadata();
    }

    @Benchmark
    public void testWriteWithPlanarImageAndIPTCMetadata() throws Exception {
        super.testWriteWithPlanarImageAndIPTCMetadata();
    }

    @Benchmark
    public void testWriteWithPlanarImageAndXMPMetadata() throws Exception {
        super.testWriteWithPlanarImageAndXMPMetadata();
    }
}
