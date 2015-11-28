package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Rotation;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Operations;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.io.FileUtils;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class FilesystemCacheTest extends CantaloupeTestCase {

    File fixturePath;
    File imagePath;
    File infoPath;
    FilesystemCache instance;

    public void setUp() throws IOException {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        fixturePath = Paths.get(cwd, "src", "test", "resources", "cache").toFile();
        imagePath = Paths.get(cwd, "src", "test", "resources", "cache", "image").toFile();
        infoPath = Paths.get(cwd, "src", "test", "resources", "cache", "info").toFile();
        FileUtils.deleteDirectory(fixturePath);
        if (!imagePath.mkdirs()) {
            throw new IOException("Failed to create temp image folder");
        }
        if (!infoPath.mkdirs()) {
            throw new IOException("Failed to create temp info folder");
        }

        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("FilesystemCache.pathname", fixturePath.toString());
        config.setProperty("FilesystemCache.ttl_seconds", 0);
        Application.setConfiguration(config);

        instance = new FilesystemCache();
    }

    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(fixturePath);
    }

    public void testFlush() throws Exception {
        Identifier identifier = new Identifier("cats");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.JPG);
        instance.getCachedImageFile(ops).createNewFile();
        instance.getCachedInfoFile(ops.getIdentifier()).createNewFile();

        identifier = new Identifier("dogs");
        crop = new Crop();
        crop.setFull(true);
        scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(15));
        ops.add(Filter.GRAY);
        ops.setOutputFormat(OutputFormat.JPG);
        instance.getCachedImageFile(ops).createNewFile();
        instance.getCachedInfoFile(ops.getIdentifier()).createNewFile();

        instance.flush();
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testFlushWithParameters() throws Exception {
        Identifier identifier = new Identifier("cats");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);

        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.JPG);

        instance.getCachedImageFile(ops).createNewFile();
        instance.getCachedInfoFile(ops.getIdentifier()).createNewFile();
        instance.flush(ops);
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testFlushExpired() throws Exception {
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);

        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);

        Operations ops = new Operations();
        ops.setIdentifier(new Identifier("cats"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.JPG);

        instance.getCachedImageFile(ops).createNewFile();
        instance.getCachedInfoFile(ops.getIdentifier()).createNewFile();

        Thread.sleep(2000);

        crop = new Crop();
        crop.setFull(true);
        scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new Operations();
        ops.setIdentifier(new Identifier("dogs"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.JPG);

        instance.getCachedImageFile(ops).createNewFile();
        instance.getCachedInfoFile(ops.getIdentifier()).createNewFile();

        instance.flushExpired();
        assertEquals(1, imagePath.listFiles().length);
        assertEquals(1, infoPath.listFiles().length);
    }

    public void testGetDimensionWithZeroTtl() throws Exception {
        Identifier identifier = new Identifier("test");
        File file = new File(infoPath + File.separator + identifier + ".json");
        FileUtils.writeStringToFile(file, "50x50");

        ObjectMapper mapper = new ObjectMapper();
        FilesystemCache.ImageInfo info = new FilesystemCache.ImageInfo();
        info.setWidth(50);
        info.setHeight(50);
        mapper.writeValue(file, info);
        assertEquals(new Dimension(50, 50), instance.getDimension(identifier));
    }

    public void testGetDimensionWithNonZeroTtl() throws Exception {
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);
        Identifier identifier = new Identifier("test");
        File file = new File(infoPath + File.separator + identifier + ".json");
        ObjectMapper mapper = new ObjectMapper();
        FilesystemCache.ImageInfo info = new FilesystemCache.ImageInfo();
        info.setWidth(50);
        info.setHeight(50);
        mapper.writeValue(file, info);

        Thread.sleep(1100);
        assertNull(instance.getDimension(identifier));
    }

    public void testGetImageInputStreamWithZeroTtl() throws Exception {
        Identifier identifier = new Identifier("cats");
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Filter filter = Filter.DEFAULT;
        OutputFormat format = OutputFormat.JPG;

        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);

        assertNull(instance.getImageInputStream(ops));

        instance.getCachedImageFile(ops).createNewFile();
        assertTrue(instance.getImageInputStream(ops) instanceof FileInputStream);
    }

    public void testGetImageInputStreamWithNonzeroTtl() throws Exception {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);

        Operations ops = new Operations();
        ops.setIdentifier(new Identifier("cats"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.JPG);

        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);
        File cacheFile = instance.getCachedImageFile(ops);
        cacheFile.createNewFile();
        assertTrue(instance.getImageInputStream(ops) instanceof FileInputStream);

        Thread.sleep(1100);
        assertNull(instance.getImageInputStream(ops));
        assertFalse(cacheFile.exists());
    }

    public void testGetImageOutputStream() throws Exception {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);

        Operations ops = new Operations();
        ops.setIdentifier(new Identifier("cats"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.JPG);

        assertTrue(instance.getImageOutputStream(ops) instanceof FileOutputStream);
    }

    public void testImageOutputStreamCreatesFolder() throws IOException {
        FileUtils.deleteDirectory(imagePath);

        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);

        Operations ops = new Operations();
        ops.setIdentifier(new Identifier("cats"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.JPG);

        instance.getImageOutputStream(ops);
        assertTrue(imagePath.exists());
    }

    public void testGetCachedImageFile() {
        String pathname = Application.getConfiguration().
                getString("FilesystemCache.pathname");

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.905f);
        Rotation rotation = new Rotation(10);
        Filter filter = Filter.DEFAULT;
        OutputFormat format = OutputFormat.TIF;

        Operations ops = new Operations();
        ops.setIdentifier(identifier);
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);

        final String search = "[^A-Za-z0-9._-]";
        final String replacement = "_";
        String expected = String.format("%s%simage%s%s_%s_%s_%s_%s.%s", pathname,
                File.separator,
                File.separator,
                identifier.toString().replaceAll(search, replacement),
                crop.toString().replaceAll(search, replacement),
                scale.toString().replaceAll(search, replacement),
                rotation.toString().replaceAll(search, replacement),
                filter.toString().toLowerCase(), format);
        assertEquals(new File(expected), instance.getCachedImageFile(ops));
    }

    public void testPutDimension() throws IOException {
        Identifier identifier = new Identifier("cats");
        Dimension dimension = new Dimension(52, 52);
        instance.putDimension(identifier, dimension);
        assertEquals(dimension, instance.getDimension(identifier));
    }

}
