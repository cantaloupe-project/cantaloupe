package edu.illinois.library.cantaloupe.image;

class NumberUtil {

    public static String removeTrailingZeroes(Float f) {
        String s = f.toString();
        return s.indexOf(".") < 0 ? s : s.replaceAll("0*$", "").
                replaceAll("\\.$", "");
    }

}
