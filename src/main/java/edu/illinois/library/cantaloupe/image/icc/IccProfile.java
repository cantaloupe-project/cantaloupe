package edu.illinois.library.cantaloupe.image.icc;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Operation;
import org.apache.commons.io.IOUtils;

import java.awt.Dimension;
import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IccProfile implements Operation {

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

    @Override
    public Dimension getResultingSize(Dimension fullSize) {
        return fullSize;
    }

    @Override
    public boolean isNoOp() {
        return false;
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

    @Override
    public Map<String, Object> toMap(Dimension fullSize) {
        final HashMap<String,Object> map = new HashMap<>();
        map.put("operation", "icc_profile");
        map.put("pathname", (getFile() != null) ?
                getFile().getAbsolutePath() : null);
        map.put("name", getName());
        return map;
    }

    /**
     * @return String representation of the instance, in the format
     * "{profile filename}".
     */
    @Override
    public String toString() {
        return (getFile() != null) ? getFile().getName() : null;
    }

}
