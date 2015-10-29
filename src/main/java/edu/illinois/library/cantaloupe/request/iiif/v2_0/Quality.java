package edu.illinois.library.cantaloupe.request.iiif.v2_0;

/**
 * Encapsulates the "quality" component of an IIIF request URI.
 *
 * @see <a href="http://iiif.io/api/image/2.0/#quality">IIIF Image API 2.0</a>
 */
public enum Quality {

    BITONAL, COLOR, DEFAULT, GRAY;

    public edu.illinois.library.cantaloupe.image.Quality toQuality() {
        return edu.illinois.library.cantaloupe.image.Quality.valueOf(this.toString());
    }

}
