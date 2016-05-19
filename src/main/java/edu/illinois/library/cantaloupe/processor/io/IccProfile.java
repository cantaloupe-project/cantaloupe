package edu.illinois.library.cantaloupe.processor.io;

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
        final FileInputStream in = new FileInputStream(file);
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
