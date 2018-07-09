package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;

import java.util.List;

interface SelectionStrategy {

    /**
     * @return Strategy from the application configuration.
     */
    static SelectionStrategy fromConfiguration() {
        final Configuration config = Configuration.getInstance();
        switch (config.getString(Key.PROCESSOR_SELECTION_STRATEGY, "")) {
            case "ManualSelectionStrategy":
                return new ManualSelectionStrategy();
            default:
                return new AutomaticSelectionStrategy();
        }
    }

    /**
     * @param sourceFormat
     * @return             List of processors for handling the given source
     *                     format, in order from most to least preferred.
     */
    List<Class<? extends Processor>> getPreferredProcessors(Format sourceFormat);

}
