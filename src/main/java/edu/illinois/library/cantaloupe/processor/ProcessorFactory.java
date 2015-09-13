package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import org.apache.commons.configuration.Configuration;

import java.util.HashSet;
import java.util.Set;

public class ProcessorFactory {

    public static Set<Processor> getAllProcessors() {
        // might be preferable to scan the package for classes implementing
        // Processor, but might also not be worth the hassle/overhead
        Set<Processor> processors = new HashSet<Processor>();
        processors.add(new GraphicsMagickProcessor());
        processors.add(new ImageIoProcessor());
        processors.add(new ImageMagickProcessor());
        processors.add(new JaiProcessor());
        return processors;
    }

    /**
     * @param sourceFormat The source format for which to return an instance,
     *                     based on configuration settings. If unsure, use
     *                     <code>SourceFormat.UNKNOWN</code>.
     * @return An instance suitable for handling the given source format, based
     * on configuration settings.
     * @throws UnsupportedSourceFormatException If the processor assigned to
     * the given source format, or the fallback processor, does not support the
     * format.
     * @throws ReflectiveOperationException If a defined processor class is
     * not found or cannot be instantiated.
     */
    public static Processor getProcessor(SourceFormat sourceFormat)
            throws UnsupportedSourceFormatException, ReflectiveOperationException {
        String processorName = getAssignedProcessorName(sourceFormat);
        boolean fallingBack = false;
        if (processorName == null) {
            processorName = getFallbackProcessorName();
            fallingBack = true;
            if (processorName == null) {
                throw new ClassNotFoundException("A fallback processor is not defined.");
            }
        }
        String className = ProcessorFactory.class.getPackage().getName() +
                "." + processorName;
        Class class_ = Class.forName(className);
        Processor processor = (Processor) class_.newInstance();

        if (!processor.getSupportedSourceFormats().contains(sourceFormat)) {
            String msg;
            if (fallingBack) {
                msg = String.format("No processor assigned to this format " +
                                "(%s), and fallback %s does not " +
                                "support it either",
                        sourceFormat.getPreferredExtension(), processorName);
            } else {
                msg = String.format("Processor assigned to %s, %s, does not support the %s format",
                        sourceFormat.getPreferredExtension(),
                        processorName, sourceFormat.getPreferredExtension());
            }
            throw new UnsupportedSourceFormatException(msg);
        }
        return processor;
    }

    private static String getAssignedProcessorName(SourceFormat sourceFormat) {
        Configuration config = Application.getConfiguration();
        return config.getString("processor." +
                sourceFormat.getPreferredExtension());
    }

    private static String getFallbackProcessorName() {
        Configuration config = Application.getConfiguration();
        return config.getString("processor.fallback");
    }

}
