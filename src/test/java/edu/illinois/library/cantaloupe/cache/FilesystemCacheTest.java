package edu.illinois.library.cantaloupe.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Quality;
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
        imagePath.mkdirs();
        infoPath.mkdirs();

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
        Crop region = new Crop();
        region.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        Operations params = new Operations(identifier, region, scale, rotation,
                quality, format);
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();

        identifier = new Identifier("dogs");
        region = new Crop();
        region.setFull(true);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        rotation = new Rotation(15);
        quality = Quality.GRAY;
        format = OutputFormat.JPG;
        params = new Operations(identifier, region, scale, rotation,
                quality, format);
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();

        instance.flush();
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testFlushWithParameters() throws Exception {
        Identifier identifier = new Identifier("cats");
        Crop region = new Crop();
        region.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        Operations params = new Operations(identifier, region, scale, rotation,
                quality, format);
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();
        instance.flush(params);
        assertEquals(0, imagePath.listFiles().length);
        assertEquals(0, infoPath.listFiles().length);
    }

    public void testFlushExpired() throws Exception {
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);

        Identifier identifier = new Identifier("cats");
        Crop region = new Crop();
        region.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        Operations params = new Operations(identifier, region, scale, rotation,
                quality, format);
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();

        Thread.sleep(2000);

        identifier = new Identifier("dogs");
        region = new Crop();
        region.setFull(true);
        scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        rotation = new Rotation(0);
        quality = Quality.DEFAULT;
        format = OutputFormat.JPG;
        params = new Operations(identifier, region, scale, rotation,
                quality, format);
        instance.getCachedImageFile(params).createNewFile();
        instance.getCachedInfoFile(params.getIdentifier()).createNewFile();

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
        Crop region = new Crop();
        region.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        Operations params = new Operations(identifier, region, scale, rotation,
                quality, format);
        assertNull(instance.getImageInputStream(params));

        instance.getCachedImageFile(params).createNewFile();
        assertTrue(instance.getImageInputStream(params) instanceof FileInputStream);
    }

    public void testGetImageInputStreamWithNonzeroTtl() throws Exception {
        Identifier identifier = new Identifier("cats");
        Crop region = new Crop();
        region.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        Operations params = new Operations(identifier, region, scale, rotation,
                quality, format);
        Application.getConfiguration().setProperty("FilesystemCache.ttl_seconds", 1);
        File cacheFile = instance.getCachedImageFile(params);
        cacheFile.createNewFile();
        assertTrue(instance.getImageInputStream(params) instanceof FileInputStream);
        Thread.sleep(1100);
        assertNull(instance.getImageInputStream(params));
        assertFalse(cacheFile.exists());
    }

    public void testGetImageOutputStream() throws Exception {
        Identifier identifier = new Identifier("cats");
        Crop region = new Crop();
        region.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        Operations params = new Operations(identifier, region, scale, rotation,
                quality, format);
        assertTrue(instance.getImageOutputStream(params) instanceof FileOutputStream);
    }

    public void testImageOutputStreamCreatesFolder() throws IOException {
        FileUtils.deleteDirectory(imagePath);

        Identifier identifier = new Identifier("cats");
        Crop region = new Crop();
        region.setFull(true);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Quality quality = Quality.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        Operations params = new Operations(identifier, region, scale, rotation,
                quality, format);
        instance.getImageOutputStream(params);
        assertTrue(imagePath.exists());
    }

    public void testGetCachedImageFile() {
        String pathname = Application.getConfiguration().
                getString("FilesystemCache.pathname");

        Identifier identifier = new Identifier("cats_~!@#$%^&*()");
        Crop region = new Crop();
        region.setWidth(50f);
        region.setHeight(50f);
        Scale scale = new Scale();
        scale.setScaleMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.905f);
        Rotation rotation = new Rotation(10);
        rotation.setMirror(true);
        Quality quality = Quality.COLOR;
        OutputFormat format = OutputFormat.TIF;
        Operations params = new Operations(identifier, region, scale, rotation,
                quality, format);
        final String search = "[^A-Za-z0-9._-]";
        final String replacement = "_";
        String expected = String.format("%s%simage%s%s_%s_%s_%s_%s.%s", pathname,
                File.separator,
                File.separator,
                identifier.toString().replaceAll(search, replacement),
                region.toString().replaceAll(search, replacement),
                scale.toString().replaceAll(search, replacement),
                rotation.toString().replaceAll(search, replacement),
                quality.toString().toLowerCase(), format);
        assertEquals(new File(expected), instance.getCachedImageFile(params));
    }

    public void testPutDimension() throws IOException {
        Identifier identifier = new Identifier("cats");
        Dimension dimension = new Dimension(52, 52);
        instance.putDimension(identifier, dimension);
        assertEquals(dimension, instance.getDimension(identifier));
    }

}
