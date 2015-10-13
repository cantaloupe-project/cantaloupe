package edu.illinois.library.cantaloupe.util;

public class NumberUtil {

    public static String removeTrailingZeroes(Float f) {
        String s = f.toString();
        return s.indexOf(".") < 0 ? s : s.replaceAll("0*$", "").
                replaceAll("\\.$", "");
    }

}
