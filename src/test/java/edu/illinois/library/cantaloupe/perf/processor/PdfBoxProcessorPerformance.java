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
public class PdfBoxProcessorPerformance {

    private static final Format OUTPUT_FORMAT = Format.PNG;

    private FileProcessor processor;

    @Setup
    public void setUp() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK, "PdfBoxProcessor");
        processor = (FileProcessor) new ProcessorFactory().newProcessor(Format.PDF);
    }

    @TearDown
    public void tearDown() {
        processor.close();
    }

    @Benchmark
    public void process() throws Exception {
        processor.setSourceFormat(Format.PDF);
        processor.setSourceFile(TestUtil.getImage("pdf"));
        processor.process(
                new OperationList(new Encode(OUTPUT_FORMAT)),
                Info.builder().withSize(64, 56).build(),
                OutputStream.nullOutputStream());
    }

    @Benchmark
    public void readInfo() throws Exception {
        processor.setSourceFormat(Format.PDF);
        processor.setSourceFile(TestUtil.getImage("pdf"));
        processor.readInfo();
    }

}
