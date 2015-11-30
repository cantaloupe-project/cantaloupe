package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

/**
 * Restlet representation for images.
 */
abstract class AbstractImageRepresentation extends OutputRepresentation {

    public AbstractImageRepresentation(MediaType mediaType,
                                       Identifier identifier,
                                       OutputFormat outputFormat) {
        super(mediaType);
        Disposition disposition = new Disposition();
        switch (Application.getConfiguration().
                getString(ImageRepresentation.CONTENT_DISPOSITION_CONFIG_KEY, "none")) {
            case "inline":
                disposition.setType(Disposition.TYPE_INLINE);
                this.setDisposition(disposition);
                break;
            case "attachment":
                disposition.setType(Disposition.TYPE_ATTACHMENT);
                disposition.setFilename(
                        identifier.toString().replaceAll(
                                ImageRepresentation.FILENAME_CHARACTERS, "_") +
                                "." + outputFormat.getExtension());
                this.setDisposition(disposition);
                break;
        }
    }

}