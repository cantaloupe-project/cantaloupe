package edu.illinois.library.cantaloupe.perf.processor;

import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
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
public class FfmpegProcessorPerformance {

    private FileProcessor processor;

    @Setup
    public void setUp() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "FfmpegProcessor");
        processor = (FileProcessor) new ProcessorFactory().newProcessor(Format.get("avi"));
    }

    @TearDown
    public void tearDown() {
        processor.close();
    }

    @Benchmark
    public void processWithAVI() throws Exception {
        processor.setSourceFormat(Format.get("avi"));
        processor.setSourceFile(TestUtil.getImage("avi"));
        processor.process(
                new OperationList(new Encode(Format.get("png"))),
                Info.builder().withSize(640, 360).build(),
                OutputStream.nullOutputStream());
    }

    @Benchmark
    public void processWithFLV() throws Exception {
        processor.setSourceFormat(Format.get("flv"));
        processor.setSourceFile(TestUtil.getImage("flv"));
        processor.process(
                new OperationList(new Encode(Format.get("png"))),
                Info.builder().withSize(640, 360).build(),
                OutputStream.nullOutputStream());
    }

    @Benchmark
    public void processWithMOV() throws Exception {
        processor.setSourceFormat(Format.get("mov"));
        processor.setSourceFile(TestUtil.getImage("mov"));
        processor.process(
                new OperationList(new Encode(Format.get("png"))),
                Info.builder().withSize(640, 360).build(),
                OutputStream.nullOutputStream());
    }

    @Benchmark
    public void processWithMP4() throws Exception {
        processor.setSourceFormat(Format.get("mp4"));
        processor.setSourceFile(TestUtil.getImage("mp4"));
        processor.process(
                new OperationList(new Encode(Format.get("png"))),
                Info.builder().withSize(640, 360).build(),
                OutputStream.nullOutputStream());
    }

    @Benchmark
    public void processWithMPG() throws Exception {
        processor.setSourceFormat(Format.get("mpg"));
        processor.setSourceFile(TestUtil.getImage("mpg"));
        processor.process(
                new OperationList(new Encode(Format.get("png"))),
                Info.builder().withSize(640, 360).build(),
                OutputStream.nullOutputStream());
    }

    @Benchmark
    public void processWithWebM() throws Exception {
        processor.setSourceFormat(Format.get("webm"));
        processor.setSourceFile(TestUtil.getImage("webm"));
        processor.process(
                new OperationList(new Encode(Format.get("png"))),
                Info.builder().withSize(640, 360).build(),
                OutputStream.nullOutputStream());
    }

    @Benchmark
    public void readInfoWithAVI() throws Exception {
        processor.setSourceFormat(Format.get("avi"));
        processor.setSourceFile(TestUtil.getImage("avi"));
        processor.readInfo();
    }

    @Benchmark
    public void readInfoWithFLV() throws Exception {
        processor.setSourceFormat(Format.get("flv"));
        processor.setSourceFile(TestUtil.getImage("flv"));
        processor.readInfo();
    }

    @Benchmark
    public void readInfoWithMOV() throws Exception {
        processor.setSourceFormat(Format.get("mov"));
        processor.setSourceFile(TestUtil.getImage("mov"));
        processor.readInfo();
    }

    @Benchmark
    public void readInfoWithMP4() throws Exception {
        processor.setSourceFormat(Format.get("mp4"));
        processor.setSourceFile(TestUtil.getImage("mp4"));
        processor.readInfo();
    }

    @Benchmark
    public void readInfoWithMPG() throws Exception {
        processor.setSourceFormat(Format.get("mpg"));
        processor.setSourceFile(TestUtil.getImage("mpg"));
        processor.readInfo();
    }

    @Benchmark
    public void readInfoWithWebM() throws Exception {
        processor.setSourceFormat(Format.get("webm"));
        processor.setSourceFile(TestUtil.getImage("webm"));
        processor.readInfo();
    }

}
