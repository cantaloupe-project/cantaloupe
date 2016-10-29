package edu.illinois.library.cantaloupe.image.watermark;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationException;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;

import java.io.File;

class BasicImageWatermarkService extends BasicWatermarkService {

    static final String FILE_CONFIG_KEY = "watermark.BasicStrategy.image";

    private File image;

    BasicImageWatermarkService() throws ConfigurationException {
        super();
        readImage();
    }

    /**
     * @return File
     */
    private File getImage() {
        return image;
    }

    ImageWatermark getWatermark() {
        return new ImageWatermark(getImage(), getPosition(), getInset());
    }

    private void readImage() throws ConfigurationException {
        final Configuration config = ConfigurationFactory.getInstance();
        final String path = config.getString(FILE_CONFIG_KEY, "");
        if (path.length() > 0) {
            image = new File(path);
        } else {
            throw new ConfigurationException(FILE_CONFIG_KEY + " is not set.");
        }
    }

}
