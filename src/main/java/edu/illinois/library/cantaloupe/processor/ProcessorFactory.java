package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import org.apache.commons.configuration.ConfigurationException;

public class ProcessorFactory {

    /**
     * @return The current image processor based on the
     * <code>processor</code> setting in the configuration. May return
     * null.
     */
    public static Processor getProcessor() {
        try {
            Class class_ = Class.forName(ProcessorFactory.class.getPackage().getName() +
                    "." + getProcessorName());
            return (Processor) class_.newInstance();
        } catch (ClassNotFoundException e) {
            return null; // TODO: log
        } catch (InstantiationException e) {
            return null; // TODO: log
        } catch (IllegalAccessException e) {
            return null; // TODO: log
        }
    }

    private static String getProcessorName() {
        String name;
        try {
            name = Application.getConfiguration().getString("processor");
        } catch (ConfigurationException e) {
            return "ImageMagickProcessor";
        }
        return name;
    }

}
