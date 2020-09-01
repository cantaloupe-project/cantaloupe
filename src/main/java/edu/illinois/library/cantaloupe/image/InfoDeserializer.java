package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;

import java.io.IOException;
import java.io.UncheckedIOException;

import static edu.illinois.library.cantaloupe.image.InfoSerializer.IDENTIFIER_KEY;
import static edu.illinois.library.cantaloupe.image.InfoSerializer.IMAGES_KEY;
import static edu.illinois.library.cantaloupe.image.InfoSerializer.MEDIA_TYPE_KEY;
import static edu.illinois.library.cantaloupe.image.InfoSerializer.METADATA_KEY;
import static edu.illinois.library.cantaloupe.image.InfoSerializer.NUM_RESOLUTIONS_KEY;

/**
 * Deserializes an {@link Info}.
 *
 * @since 5.0
 */
final class InfoDeserializer extends JsonDeserializer<Info> {

    @Override
    public Info deserialize(JsonParser parser,
                            DeserializationContext deserializationContext) throws IOException {
        final Info info = new Info();
        final JsonNode node = parser.getCodec().readTree(parser);
        { // identifier (does not exist in <= 3.4 serializations)
            JsonNode identifierNode = node.get(IDENTIFIER_KEY);
            if (identifierNode != null) {
                info.setIdentifier(new Identifier(identifierNode.textValue()));
            }
        }
        { // mediaType (always exists)
            info.setMediaType(new MediaType(node.get(MEDIA_TYPE_KEY).textValue()));
        }
        { // numResolutions (does not exist in < 4.0 serializations)
            JsonNode numResolutionsNode = node.get(NUM_RESOLUTIONS_KEY);
            if (numResolutionsNode != null) {
                info.setNumResolutions(numResolutionsNode.intValue());
            }
        }
        { // images (>= 1 always exists)
            info.getImages().clear();
            node.get(IMAGES_KEY).elements().forEachRemaining(imageNode -> {
                try {
                    Info.Image image = new ObjectMapper().readValue(
                            imageNode.toString(), Info.Image.class);
                    info.getImages().add(image);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        {   // metadata (does not exist in < 5.0 serializations
            JsonNode metadataNode = node.get(METADATA_KEY);
            if (metadataNode != null) {
                Metadata metadata = new ObjectMapper().readValue(
                        metadataNode.toString(), Metadata.class);
                info.setMetadata(metadata);
            }
        }
        return info;
    }

}
