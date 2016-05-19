package edu.illinois.library.cantaloupe.processor.io;

import edu.illinois.library.cantaloupe.config.Configuration;
import org.apache.commons.io.IOUtils;

import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class IccProfile {

    private File file;
    private String name;

    /**
     * @param name Name of the profile.
     * @param profileFile File representing the profile on disk.
     */
    IccProfile(String name, File profileFile) {
        setName(name);
        setFile(profileFile);
    }

    /**
     * Finds the profile whether {@link #getFile} returns an absolute path or
     * a filename. If the latter, it will be searched for in the same directory
     * as the application config (if available), or the current working
     * directory if not.
     *
     * @return File whose {@link File#isAbsolute()} method returns
     *         <code>true</code>
     */
    private File findProfile() {
        File absoluteFile = file;
        if (!file.isAbsolute()) {
            final File configFile =
                    Configuration.getInstance().getConfigurationFile();
            if (configFile != null) {
                absoluteFile = new File(configFile.getParent() + "/" +
                        file.getName());
            } else {
                absoluteFile = new File("./" + file.getName());
            }
        }
        return absoluteFile;
    }

    /**
     * @return File representing the profile on disk.
     */
    public File getFile() {
        return file;
    }

    /**
     * @return Name of the profile.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Instance corresponding to the file returned by {@link #getFile}.
     * @throws IOException
     */
    public ICC_Profile getProfile() throws IOException {
        final FileInputStream in = new FileInputStream(findProfile());
        try {
            return ICC_Profile.getInstance(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * @param name Name of the profile.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param file File representing the profile on disk.
     */
    public void setFile(File file) {
        this.file = file;
    }

}
