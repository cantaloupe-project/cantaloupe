package edu.illinois.library.cantaloupe.resource.iiif;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.resource.ImageRepresentation;
import org.restlet.data.Disposition;

/**
 * Contains IIIF resource helper methods.
 */
public abstract class ResourceUtils {

    public static final String CONTENT_DISPOSITION_CONFIG_KEY =
            "endpoint.iiif.content_disposition";

    /**
     * @param identifier
     * @param outputFormat
     * @return A content disposition based on the setting of
     * {@link #CONTENT_DISPOSITION_CONFIG_KEY} in the application configuration.
     * If it is set to <code>attachment</code>, the disposition will have a
     * filename set to a reasonable value based on the given identifier and
     * output format.
     */
    public static Disposition getRepresentationDisposition(
            Identifier identifier, OutputFormat outputFormat) {
        Disposition disposition = new Disposition();
        switch (Application.getConfiguration().
                getString(CONTENT_DISPOSITION_CONFIG_KEY, "none")) {
            case "inline":
                disposition.setType(Disposition.TYPE_INLINE);
                break;
            case "attachment":
                disposition.setType(Disposition.TYPE_ATTACHMENT);
                disposition.setFilename(
                        identifier.toString().replaceAll(
                                ImageRepresentation.FILENAME_CHARACTERS, "_") +
                                "." + outputFormat.getExtension());
                break;
        }
        return disposition;
    }

}
