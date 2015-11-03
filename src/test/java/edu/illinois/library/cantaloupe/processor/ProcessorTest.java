package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operations;
import edu.illinois.library.cantaloupe.image.Quality;
import edu.illinois.library.cantaloupe.image.Rotation;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contains base tests common to all Processors.
 */
public abstract class ProcessorTest extends CantaloupeTestCase {

    static {
        Application.setConfiguration(new BaseConfiguration());
    }

    protected SourceFormat getAnySupportedSourceFormat(Processor processor) {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (processor.getAvailableOutputFormats(sourceFormat).size() > 0) {
                return sourceFormat;
            }
        }
        return null;
    }

    protected abstract Processor getProcessor();

    public void testGetSize() throws Exception {
        Dimension expectedSize = new Dimension(594, 522);
        if (getProcessor() instanceof StreamProcessor) {
            StreamProcessor proc = (StreamProcessor) getProcessor();
            try (InputStream inputStream = new FileInputStream(
                    TestUtil.getFixture("escher_lego.jpg"))) {
                Dimension actualSize = proc.getSize(inputStream, SourceFormat.JPG);
                assertEquals(expectedSize, actualSize);
            }
        }
        if (getProcessor() instanceof FileProcessor) {
            FileProcessor proc = (FileProcessor) getProcessor();
            Dimension actualSize = proc.getSize(
                    TestUtil.getFixture("escher_lego.jpg"), SourceFormat.JPG);
            assertEquals(expectedSize, actualSize);
        }
    }

    public void testProcessWithSupportedSourceFormatsAndNoTransformation() throws Exception {
        Identifier identifier = new Identifier("bla");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        Operations params = new Operations(identifier, crop, scale, rotation,
                quality, format);
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                if (getProcessor() instanceof StreamProcessor) {
                    InputStream processInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    InputStream sizeInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    try {
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        Dimension size = proc.getSize(sizeInputStream, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, processInputStream,
                                outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    } finally {
                        processInputStream.close();
                        sizeInputStream.close();
                    }
                }
                if (getProcessor() instanceof FileProcessor) {
                    FileProcessor proc = (FileProcessor) getProcessor();
                    File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
                    Dimension size = proc.getSize(file, sourceFormat);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    proc.process(params, sourceFormat, size, file, outputStream);
                    assertTrue(outputStream.toByteArray().length > 100);
                }
            }
        }
    }

    public void testProcessWithUnsupportedSourceFormats() throws Exception {
        Identifier identifier = new Identifier("bla");
        Crop crop = new Crop();
        crop.setX(20f);
        crop.setY(20f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.8f);
        Rotation rotation = new Rotation(15);
        Quality quality = Quality.COLOR;
        OutputFormat format = OutputFormat.JPG;
        Operations params = new Operations(identifier, crop, scale, rotation,
                quality, format);
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (getProcessor().getAvailableOutputFormats(sourceFormat).size() == 0) {
                if (getProcessor() instanceof StreamProcessor) {
                    InputStream sizeInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    InputStream processInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    try {
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        Dimension size = proc.getSize(sizeInputStream, sourceFormat);
                        proc.process(params, sourceFormat, size,
                                processInputStream, new NullOutputStream());
                        fail("Expected exception");
                    } catch (ProcessorException e) {
                        assertEquals("Unsupported source format: " +
                                        sourceFormat.getPreferredExtension(),
                                e.getMessage());
                    } finally {
                        sizeInputStream.close();
                        processInputStream.close();
                    }
                }
                if (getProcessor() instanceof FileProcessor) {
                    try {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = TestUtil.getFixture(
                                sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        proc.process(params, sourceFormat, size, file,
                                new NullOutputStream());
                        fail("Expected exception");
                    } catch (ProcessorException e) {
                        assertEquals("Unsupported source format: " +
                                sourceFormat.getPreferredExtension(),
                                e.getMessage());
                    }
                }
            }
        }
    }

    public void testProcessWithRegionTransformation() throws Exception {
        List<Crop> regions = new ArrayList<>();
        Crop crop = new Crop();
        crop.setFull(true);
        regions.add(crop);

        crop = new Crop();
        crop.setX(10f);
        crop.setY(10f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        regions.add(crop);

        crop = new Crop();
        crop.setPercent(true);
        crop.setX(20f);
        crop.setY(20f);
        crop.setWidth(20f);
        crop.setHeight(20f);
        regions.add(crop);

        for (Crop region : regions) {
            Identifier identifier = new Identifier("bla");
            Scale scale = new Scale();
            scale.setScaleMode(Scale.Mode.FULL);
            Rotation rotation = new Rotation(0);
            Quality quality = Quality.DEFAULT;
            OutputFormat format = OutputFormat.JPG;
            Operations params = new Operations(identifier, region, scale,
                    rotation, quality, format);
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                    if (getProcessor() instanceof StreamProcessor) {
                        InputStream sizeInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension size = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, sourceFormat, size,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, file, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

    public void testProcessWithSizeTransformation() throws Exception {
        List<Scale> scales = new ArrayList<>();
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        scales.add(scale);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(20);
        scales.add(scale);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(20);
        scales.add(scale);
        scale = new Scale();
        scale.setPercent(0.5f);
        scales.add(scale);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(20);
        scale.setHeight(20);
        scales.add(scale);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.NON_ASPECT_FILL);
        scale.setWidth(20);
        scale.setHeight(20);
        scales.add(scale);

        for (Scale size : scales) {
            Identifier identifier = new Identifier("bla");
            Crop crop = new Crop();
            crop.setX(10f);
            crop.setY(10f);
            crop.setWidth(50f);
            crop.setHeight(50f);
            Rotation rotation = new Rotation(0);
            Quality quality = Quality.DEFAULT;
            OutputFormat format = OutputFormat.JPG;
            Operations ops = new Operations(identifier, crop, size, rotation,
                    quality, format);
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                    if (getProcessor() instanceof StreamProcessor) {
                        InputStream sizeInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension fullSize = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(ops, sourceFormat, fullSize,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
                        Dimension fullSize = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(ops, sourceFormat, fullSize, file, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

    public void testProcessWithRotationTransformation() throws Exception {
        List<Rotation> rotations = new ArrayList<>();
        Rotation rot = new Rotation(0);
        rotations.add(rot);
        rot = new Rotation(15);
        rotations.add(rot);
        rot = new Rotation(275);
        rotations.add(rot);
        rot = new Rotation(15);
        rot.setMirror(true);
        rotations.add(rot);

        for (Rotation rotation : rotations) {
            Identifier identifier = new Identifier("bla");
            Crop crop = new Crop();
            crop.setX(10f);
            crop.setY(10f);
            crop.setWidth(50f);
            crop.setHeight(50f);
            Scale scale = new Scale();
            scale.setWidth(20);
            scale.setHeight(20);
            scale.setScaleMode(Scale.Mode.ASPECT_FIT_INSIDE);
            Quality quality = Quality.DEFAULT;
            OutputFormat format = OutputFormat.JPG;
            Operations params = new Operations(identifier, crop, scale,
                    rotation, quality, format);
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                    if (getProcessor() instanceof StreamProcessor) {
                        InputStream sizeInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension size = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, sourceFormat, size,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, file, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

    public void testProcessWithQualityTransformation() throws Exception {
        List<Quality> qualities = new ArrayList<>();
        qualities.add(Quality.DEFAULT);
        qualities.add(Quality.COLOR);
        qualities.add(Quality.GRAY);
        qualities.add(Quality.BITONAL);

        for (Quality quality : qualities) {
            Identifier identifier = new Identifier("bla");
            Crop crop = new Crop();
            crop.setX(10f);
            crop.setY(10f);
            crop.setWidth(50f);
            crop.setHeight(50f);
            Scale scale = new Scale();
            scale.setWidth(20);
            scale.setHeight(20);
            scale.setScaleMode(Scale.Mode.ASPECT_FIT_INSIDE);
            Rotation rotation = new Rotation(10);
            OutputFormat format = OutputFormat.JPG;
            Operations params = new Operations(identifier, crop, scale,
                    rotation, quality, format);
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                    if (getProcessor() instanceof StreamProcessor) {
                        InputStream sizeInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension size = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, sourceFormat, size,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, file, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

    public void testProcessWithSupportedOutputFormats() throws Exception {
        Set<OutputFormat> outputFormats = getProcessor().
                getAvailableOutputFormats(SourceFormat.JPG);
        for (OutputFormat outputFormat : outputFormats) {
            Identifier identifier = new Identifier("bla");
            Crop crop = new Crop();
            crop.setX(10f);
            crop.setY(10f);
            crop.setWidth(50f);
            crop.setHeight(50f);
            Scale scale = new Scale();
            scale.setWidth(20);
            scale.setHeight(20);
            scale.setScaleMode(Scale.Mode.ASPECT_FIT_INSIDE);
            Rotation rotation = new Rotation(10);
            Quality quality = Quality.DEFAULT;
            OutputFormat format = OutputFormat.JPG;
            Operations params = new Operations(identifier, crop, scale,
                    rotation, quality, format);
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                    if (getProcessor() instanceof StreamProcessor) {
                        InputStream sizeInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension size = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, sourceFormat, size,
                                    processInputStream, outputStream);
                            System.out.println(outputFormat.getExtension());
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, file,
                                outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

}
