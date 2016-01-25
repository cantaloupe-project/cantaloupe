package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Used to obtain an instance of a {@link Processor} for a given source format,
 * as defined in the configuration.
 */
public abstract class ProcessorFactory {

    private static Logger logger = LoggerFactory.
            getLogger(ProcessorFactory.class);

    public static Set<Processor> getAllProcessors() {
        // might be preferable to scan the package for classes implementing
        // Processor, but might also not be worth the hassle
        Set<Processor> processors = new HashSet<>();
        processors.add(new FfmpegProcessor());
        processors.add(new GraphicsMagickProcessor());
        processors.add(new ImageMagickProcessor());
        processors.add(new JaiProcessor());
        processors.add(new Java2dProcessor());
        processors.add(new KakaduProcessor());
        processors.add(new OpenJpegProcessor());
        return processors;
    }

    /**
     * Alternative to {@link #getProcessor(SourceFormat, Resolver)} that
     * doesn't take a resolver parameter, nor throw an
     * IncompatibleResolverException.
     *
     * @param sourceFormat
     * @return
     * @throws UnsupportedSourceFormatException
     * @throws ReflectiveOperationException
     */
    public static Processor getProcessor(final SourceFormat sourceFormat)
            throws UnsupportedSourceFormatException,
            ReflectiveOperationException {
        Processor processor = null;
        try {
            processor = getProcessor(sourceFormat, null);
        } catch (IncompatibleResolverException e) {
            // this will never happen
            logger.error("BUG ALERT in getProcessor(SourceFormat)", e);
        }
        return processor;
    }

    /**
     * @param sourceFormat The source format for which to return an instance,
     *                     based on configuration settings. If unsure, use
     *                     <code>SourceFormat.UNKNOWN</code>.
     * @param resolver Resolver from which the processor will be reading
     * @return An instance suitable for handling the given source format, based
     * on configuration settings.
     * @throws IncompatibleResolverException
     * @throws ClassNotFoundException If a fallback processor is needed but not
     * defined.
     * @throws UnsupportedSourceFormatException If the processor assigned to
     * the given source format, or the fallback processor, does not support the
     * format.
     * @throws ReflectiveOperationException If a defined processor class is
     * not found or cannot be instantiated.
     */
    public static Processor getProcessor(final SourceFormat sourceFormat,
                                         final Resolver resolver)
            throws IncompatibleResolverException,
            UnsupportedSourceFormatException,
            ReflectiveOperationException {
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

        if (resolver != null && !resolver.isCompatible(processor)) {
            throw new IncompatibleResolverException(resolver, processor);
        }

        if (processor.getAvailableOutputFormats(sourceFormat).size() < 1) {
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
