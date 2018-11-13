package edu.illinois.library.cantaloupe.perf.processor;

import java.util.concurrent.TimeUnit;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.output.NullOutputStream;
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
public class ImageMagickProcessorPerformance {

    private static final Format OUTPUT_FORMAT = Format.PNG;

    private StreamProcessor processor;

    @Setup
    public void setUp() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "ImageMagickProcessor");
        processor = (StreamProcessor) new ProcessorFactory().newProcessor(Format.BMP);
    }

    @TearDown
    public void tearDown() {
        processor.close();
    }

    @Benchmark
    public void processWithBMP() throws Exception {
        processor.setSourceFormat(Format.BMP);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("bmp-rgb-64x56x8.bmp")));
        processor.process(
                new OperationList(new Encode(OUTPUT_FORMAT)),
                Info.builder().withSize(64, 56).build(),
                new NullOutputStream());
    }

    @Benchmark
    public void processWithGIF() throws Exception {
        processor.setSourceFormat(Format.GIF);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("gif-rgb-64x56x8.gif")));
        processor.process(
                new OperationList(new Encode(OUTPUT_FORMAT)),
                Info.builder().withSize(64, 56).build(),
                new NullOutputStream());
    }

    @Benchmark
    public void processWithJPG() throws Exception {
        processor.setSourceFormat(Format.JPG);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("jpg-rgb-64x56x8-line.jpg")));
        processor.process(
                new OperationList(new Encode(OUTPUT_FORMAT)),
                Info.builder().withSize(64, 56).build(),
                new NullOutputStream());
    }

    @Benchmark
    public void processWithPNG() throws Exception {
        processor.setSourceFormat(Format.PNG);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("png-rgb-64x56x8.png")));
        processor.process(
                new OperationList(new Encode(OUTPUT_FORMAT)),
                Info.builder().withSize(64, 56).build(),
                new NullOutputStream());
    }

    @Benchmark
    public void processWithTIF() throws Exception {
        processor.setSourceFormat(Format.TIF);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("tif-rgb-1res-64x56x8-striped-lzw.tif")));
        processor.process(
                new OperationList(new Encode(OUTPUT_FORMAT)),
                Info.builder().withSize(64, 56).build(),
                new NullOutputStream());
    }

    @Benchmark
    public void readInfoWithBMP() throws Exception {
        processor.setSourceFormat(Format.BMP);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("bmp-rgb-64x56x8.bmp")));
        processor.readInfo();
    }

    @Benchmark
    public void readInfoWithGIF() throws Exception {
        processor.setSourceFormat(Format.GIF);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("gif-rgb-64x56x8.gif")));
        processor.readInfo();
    }

    @Benchmark
    public void readInfoWithJPG() throws Exception {
        processor.setSourceFormat(Format.JPG);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("jpg-rgb-64x56x8-line.jpg")));
        processor.readInfo();
    }

    @Benchmark
    public void readInfoWithPNG() throws Exception {
        processor.setSourceFormat(Format.PNG);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("png-rgb-64x56x8.png")));
        processor.readInfo();
    }

    @Benchmark
    public void readInfoWithTIF() throws Exception {
        processor.setSourceFormat(Format.TIF);
        processor.setStreamFactory(new PathStreamFactory(TestUtil.getImage("tif-rgb-1res-64x56x8-striped-lzw.tif")));
        processor.readInfo();
    }


}
