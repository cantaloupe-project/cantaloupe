package edu.illinois.library.cantaloupe.image.exif;

import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFGPSTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFInteroperabilityTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFTagSet;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Set of {@link Tag}s in a {@link Directory}.
 */
public enum TagSet {

    BASELINE_TIFF   (0, "Baseline TIFF"),
    EXIF            (34665, "EXIF"),
    GPS             (34853, "GPS"),
    INTEROPERABILITY(40965, "Interoperability");

    private int ifdPointerTag;
    private String name;

    /**
     * Converts a {@link TIFFTagSet} into an instance.
     *
     * @return Instance equivalent to the argument, or {@literal null} if the
     *         argument's tag set is not recognized.
     */
    static TagSet forTIFFTagSet(Class<? extends TIFFTagSet> tiffTagSet) {
        if (tiffTagSet == BaselineTIFFTagSet.class) {
            return BASELINE_TIFF;
        } else if (tiffTagSet == EXIFTIFFTagSet.class) {
            return EXIF;
        } else if (tiffTagSet == EXIFGPSTagSet.class) {
            return GPS;
        } else if (tiffTagSet == EXIFInteroperabilityTagSet.class) {
            return INTEROPERABILITY;
        }
        return null;
    }

    /**
     * @param tagNum Tag of an IFD pointer a.k.a. offset field.
     * @return       Instance corresponding to the given tag, or {@literal
     *               null} if the given tag is not recognized.
     */
    static TagSet forIFDPointerTag(int tagNum) {
        return Arrays.stream(TagSet.values())
                .filter(ifd -> ifd.ifdPointerTag == tagNum)
                .findFirst()
                .orElse(null);
    }

    TagSet(int ifdPointerTag, String name) {
        this.ifdPointerTag = ifdPointerTag;
        this.name = name;
    }

    /**
     * @return Whether the instance contains a tag with the given value.
     */
    boolean containsTag(int tagNum) {
        return Arrays.stream(Tag.values())
                .anyMatch(t -> t.getTagSet().equals(this) && t.getID() == tagNum);
    }

    /**
     * @return Tag in the parent IFD that refers to this sub-IFD.
     */
    int getIFDPointerTag() {
        return ifdPointerTag;
    }

    public String getName() {
        return name;
    }

    /**
     * @return Tag from the set matching the given value, or {@literal null} if
     *         the set does not contain such a tag.
     */
    Tag getTag(int tagNum) {
        return Arrays.stream(Tag.values())
                .filter(t -> t.getTagSet().equals(this) && t.getID() == tagNum)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return All tags in the set.
     */
    Set<Tag> getTags() {
        return Arrays.stream(Tag.values())
                .filter(t -> this.equals(t.getTagSet()))
                .collect(Collectors.toSet());
    }

}
