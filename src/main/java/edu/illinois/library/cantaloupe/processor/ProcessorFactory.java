package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

public class ProcessorFactory {

    /**
     * @param sourceFormat The source format for which to return an instance,
     *                     based on configuration settings. If unsure, use
     *                     <code>SourceFormat.UNKNOWN</code>.
     * @return An instance suitable for handling the given source format, based
     * on configuration settings. May return null.
     */
    public static Processor getProcessor(SourceFormat sourceFormat) {
        try {
            String className = ProcessorFactory.class.getPackage().getName() +
                    "." + getProcessorName(sourceFormat);
            Class class_ = Class.forName(className);
            return (Processor) class_.newInstance();
        } catch (ClassNotFoundException e) {
            return null; // TODO: log
        } catch (InstantiationException e) {
            return null; // TODO: log
        } catch (IllegalAccessException e) {
            return null; // TODO: log
        }
    }

    private static String getProcessorName(SourceFormat sourceFormat) {
        String name;
        try {
            Configuration config = Application.getConfiguration();
            name = config.getString("processor." + sourceFormat.getExtension());
            if (name == null) {
                name = config.getString("processor.fallback");
            }
        } catch (ConfigurationException e) {
            return "ImageMagickProcessor";
        }
        return name;
    }

}
