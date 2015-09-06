package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.apache.commons.configuration.Configuration;

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
        Configuration config = Application.getConfiguration();
        String name = config.
                getString("processor." + sourceFormat.getExtension());
        if (name == null) {
            name = config.getString("processor.fallback", "ImageIoProcessor");
        }
        return name;
    }

}
