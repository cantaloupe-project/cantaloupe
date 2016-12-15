package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.operation.OperationList;

/**
 * Encapsulates a list of endpoint request parameters.
 */
public interface ParameterList {

    /**
     * @return Analog of the request parameters for processing, excluding any
     * additional operations that may need to be performed, such as overlays,
     * etc.
     */
    OperationList toOperationList();

}
