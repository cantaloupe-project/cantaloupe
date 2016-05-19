package edu.illinois.library.cantaloupe.processor.io;

import org.apache.commons.io.IOUtils;

import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Wraps an {@link ICC_Profile} instance, adding a name accessor.
 */
public class IccProfile {

    private File file;
    private String name;

    IccProfile(String name, File profileFile) {
        this.name = name;
        this.file = profileFile;
    }

    public File getFile() {
        return file;
    }

    String getName() {
        return name;
    }

    /**
     * <p>Returns an instance corresponding to the filename or pathname of an
     * ICC profile. If a filename is given, it will be searched for in the
     * same folder as the application config (if available), or the current
     * working directory if not.</p>
     *
     * @return
     * @throws IOException
     */
    ICC_Profile getProfile() throws IOException {
        final FileInputStream in = new FileInputStream(file);
        try {
            return ICC_Profile.getInstance(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

}
