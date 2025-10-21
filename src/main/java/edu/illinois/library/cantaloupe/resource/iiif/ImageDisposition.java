package edu.illinois.library.cantaloupe.resource.iiif;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.util.StringUtils;
import edu.illinois.library.cantaloupe.resource.Request;

public class ImageDisposition {
    
    static final String RESPONSE_CONTENT_DISPOSITION_QUERY_ARG =
            "response-content-disposition";


    /**
     * <p>Returns a sanitized value for a {@code Content-Disposition} header
     * based on the value of the {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG}
     * query argument.</p>
     *
     * <p>If the disposition is {@code attachment} and the filename is not
     * set, it will be set to a reasonable value based on the given identifier
     * and output format.</p>
     *
     * @return Value for a {@code Content-Disposition} header, which may be
     *         {@code null}.
     */
    public static String getRepresentationDisposition(Request request,
                                                  String identifierStr,
                                                  Format outputFormat) {
        var queryArg = request.getReference().getQuery()
                .getFirstValue(RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        return getSafeContentDisposition(queryArg, identifierStr, outputFormat);
    }

    /**
     * <p>Returns a sanitized value for a {@code Content-Disposition} header
     * based on the value of the {@link #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG}
     * query argument.</p>
     *
     * <p>If the disposition is {@code attachment} and the filename is not
     * set, it is set to a reasonable value based on the given identifier and
     * output format.</p>
     *
     * @param queryArg      Value of the unsanitized {@link
     *                      #RESPONSE_CONTENT_DISPOSITION_QUERY_ARG} query
     *                      argument.
     * @param identifierStr Identifier or meta-identifier.
     * @param outputFormat  Output format.
     * @return              Value for a {@code Content-Disposition} header,
     *                      which may be {@code null}.
     */
    private static String getSafeContentDisposition(String queryArg,
                                                    String identifierStr,
                                                    Format outputFormat) {
        String disposition = null;
        if (queryArg != null) {
            queryArg = URLDecoder.decode(queryArg, StandardCharsets.UTF_8);
            if (queryArg.startsWith("inline")) {
                disposition = "inline; filename=\"" +
                        safeContentDispositionFilename(identifierStr, outputFormat) + "\"";
            } else if (queryArg.startsWith("attachment")) {
                final List<String> dispositionParts = new ArrayList<>(3);
                dispositionParts.add("attachment");

                // Check for ISO-8859-1 filename pattern
                Pattern pattern = Pattern.compile(".*filename=\"?([^\"]*)\"?.*");
                Matcher matcher = pattern.matcher(queryArg);
                String filename;
                if (matcher.matches()) {
                    // Filter out filename-unsafe characters as well as "..".
                    filename = StringUtils.sanitize(
                            matcher.group(1),
                            Pattern.compile("\\.\\."),
                            Pattern.compile(StringUtils.ASCII_FILENAME_UNSAFE_REGEX));
                } else {
                    filename = safeContentDispositionFilename(identifierStr,
                            outputFormat);
                }
                dispositionParts.add("filename=\"" + filename + "\"");

                // Check for Unicode filename pattern
                pattern = Pattern.compile(".*filename\\*= ?(utf-8|UTF-8)''([^\"]*).*");
                matcher = pattern.matcher(queryArg);
                if (matcher.matches()) {
                    // Filter out filename-unsafe characters as well as "..".
                    filename = StringUtils.sanitize(
                            matcher.group(2),
                            Pattern.compile("\\.\\."),
                            Pattern.compile(StringUtils.UNICODE_FILENAME_UNSAFE_REGEX,
                                    Pattern.UNICODE_CHARACTER_CLASS));
                    filename = Reference.encode(filename);
                    dispositionParts.add("filename*= UTF-8''" + filename);
                }
                disposition = String.join("; ", dispositionParts);
            }
        }
        return disposition;
    }

    private static String safeContentDispositionFilename(String identifierStr,
                                                         Format outputFormat) {
        return identifierStr.replaceAll(StringUtils.ASCII_FILENAME_UNSAFE_REGEX, "_") +
                "." + outputFormat.getPreferredExtension();
    }
}
