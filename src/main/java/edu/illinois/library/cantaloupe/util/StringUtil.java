package edu.illinois.library.cantaloupe.util;

public abstract class StringUtil {

    /**
     * @param f
     * @return String representation of the given float with trailing zeroes
     *         removed.
     */
    public static String removeTrailingZeroes(Float f) {
        String s = f.toString();
        return s.indexOf(".") < 0 ? s : s.replaceAll("0*$", "").
                replaceAll("\\.$", "");
    }

}
