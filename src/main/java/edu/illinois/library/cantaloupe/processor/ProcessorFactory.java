package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessorFactory {

    private static Logger logger = LoggerFactory.getLogger(ProcessorFactory.class);

    /**
     * @param sourceFormat The source format for which to return an instance,
     *                     based on configuration settings. If unsure, use
     *                     <code>SourceFormat.UNKNOWN</code>.
     * @return An instance suitable for handling the given source format, based
     * on configuration settings. May return null.
     * @throws UnsupportedSourceFormatException
     */
    public static Processor getProcessor(SourceFormat sourceFormat)
            throws UnsupportedSourceFormatException {
        try {
            String className = ProcessorFactory.class.getPackage().getName() +
                    "." + getProcessorName(sourceFormat);
            Class class_ = Class.forName(className);
            return (Processor) class_.newInstance();
        } catch (Exception e) {
            throw new UnsupportedSourceFormatException("Unsupported source format");
        }
    }

    private static String getProcessorName(SourceFormat sourceFormat) {
        Configuration config = Application.getConfiguration();
        String name = config.
                getString("processor." + sourceFormat.getExtension());
        if (name == null) {
            name = config.getString("processor.fallback", "ImageIoProcessor");
        }
        return name;
    }

}
