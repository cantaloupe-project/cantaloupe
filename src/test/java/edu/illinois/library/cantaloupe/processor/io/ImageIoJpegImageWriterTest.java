package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.icc.IccProfile;
import edu.illinois.library.cantaloupe.image.icc.IccProfileService;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

import static org.junit.Assert.*;

public class ImageIoJpegImageWriterTest {

    private BufferedImage bufferedImage;
    private FileOutputStream outputStream;
    private PlanarImage planarImage;
    private File tempFile;
    private ImageIoJpegImageWriter writer;

    @Before
    public void setUp() throws Exception {
        // Disable ICC profiles (will be re-enabled in certain tests)
        final Configuration config = Configuration.getInstance();
        config.setProperty(IccProfileService.ICC_ENABLED_CONFIG_KEY, false);

        // Read an image fixture into memory
        final File fixture = TestUtil.getImage("jpg");
        bufferedImage = ImageIO.read(fixture);
        planarImage = JAI.create("ImageRead", fixture);

        // Instantiate a writer
        IccProfile profile = new IccProfileService().
                getProfile(new Identifier("cats"), null, "127.0.0.1");
        OperationList opList = new OperationList();
        opList.add(profile);
        writer = new ImageIoJpegImageWriter(opList);

        // Create a temp file to write to
        tempFile = File.createTempFile("test", "tmp");

        outputStream = new FileOutputStream(tempFile);
    }

    @After
    public void tearDown() throws Exception {
        outputStream.close();
        tempFile.delete();
    }

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        writer.write(bufferedImage, outputStream);
        ImageIO.read(tempFile);
    }

    @Test
    public void testWriteWithBufferedImageAndIccProfile()  throws Exception {
        configureIccProfile();
        writer.write(bufferedImage, outputStream);
        checkForIccProfile();
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        writer.write(planarImage, outputStream);
        ImageIO.read(tempFile);
    }

    @Test
    public void testWriteWithPlanarImageAndIccProfile() throws Exception {
        configureIccProfile();
        writer.write(planarImage, outputStream);
        checkForIccProfile();
    }

    private void configureIccProfile() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(IccProfileService.ICC_ENABLED_CONFIG_KEY, true);
        config.setProperty(IccProfileService.ICC_STRATEGY_CONFIG_KEY,
                "BasicStrategy");
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY,
                "test");
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY,
                TestUtil.getFixture("AdobeRGB1998.icc").getAbsolutePath());
    }

    private void checkForIccProfile() throws Exception {
        // Read it back in
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = readers.next();
        try (ImageInputStream ios = ImageIO.createImageInputStream(tempFile)) {
            reader.setInput(ios);
            // Check for the profile in its metadata
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            final Node iccNode = tree.getChildNodes().item(0).getChildNodes().
                    item(0).getChildNodes().item(0);
            assertEquals("app2ICC", iccNode.getNodeName());
        } finally {
            reader.dispose();
        }
    }

}
