package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects {@link Processor}s based on the settings of {@literal processor.*}
 * and {@link Key#PROCESSOR_FALLBACK} keys in the application configuration.
 */
class ManualSelectionStrategy implements SelectionStrategy {

    static final String CONFIGURATION_VALUE =
            ManualSelectionStrategy.class.getSimpleName();

    @Override
    @SuppressWarnings("unchecked")
    public List<Class<? extends Processor>> getPreferredProcessors(Format sourceFormat) {
        final List<Class<? extends Processor>> candidates = new ArrayList<>();

        for (String processorName : new String[] {
                getAssignedProcessorName(sourceFormat),
                getFallbackProcessorName() }) {
            if (processorName == null || processorName.isEmpty()) {
                continue;
            }
            // If the processor name contains a dot, assume it includes the
            // package name.
            final String className = processorName.contains(".") ?
                    processorName :
                    Processor.class.getPackage().getName() + "." + processorName;

            try {
                final Class<?> class_ = Class.forName(className);
                candidates.add((Class<? extends Processor>) class_);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(
                        className + " is not a recognized processor.");
            }
        }
        return candidates;
    }

    /**
     * @return Name of the processor assigned to the given format, or {@literal
     *         null} if one is not set.
     */
    private String getAssignedProcessorName(Format format) {
        final String value = Configuration.getInstance().
                getString("processor." + format.getPreferredExtension());
        return (value != null && !value.isEmpty()) ? value : null;
    }

    private String getFallbackProcessorName() {
        return Configuration.getInstance().getString(Key.PROCESSOR_FALLBACK);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
