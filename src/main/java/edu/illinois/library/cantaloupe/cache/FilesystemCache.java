package edu.illinois.library.cantaloupe.cache;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.request.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class FilesystemCache implements Cache {

    private static Logger logger = LoggerFactory.getLogger(FilesystemCache.class);

    public InputStream get(Parameters params) {
        File cacheFile = getCacheFile(params);
        if (cacheFile != null) {
            try {
                logger.debug("Hit: {}", params);
                return new FileInputStream(cacheFile);
            } catch (FileNotFoundException e) {
                // noop
            }
        }
        return null;
    }

    public OutputStream getOutputStream(Parameters params) throws IOException {
        if (get(params) == null) {
            logger.debug("Miss; caching {}", params);
            File cacheFile = getCacheFile(params);
            cacheFile.getParentFile().mkdirs();
            cacheFile.createNewFile();
            return new FileOutputStream(cacheFile);
        }
        return null;
    }

    /**
     * @param params Request parameters
     * @return File corresponding to the given parameters, or null if
     * <code>FilesystemCache.pathname</code> is not set in the configuration.
     */
    public File getCacheFile(Parameters params) {
        String dir = Application.getConfiguration().
                getString("FilesystemCache.pathname");
        if (dir != null) {
            String pathname = String.format("%s%s%s_%s_%s_%s_%s.%s",
                    StringUtils.stripEnd(dir, File.separator), File.separator,
                    params.getIdentifier(), params.getRegion(),
                    params.getSize(), params.getRotation(),
                    params.getQuality().toString().toLowerCase(),
                    params.getOutputFormat().getExtension());
            return new File(pathname);
        }
        return null;
    }

}
