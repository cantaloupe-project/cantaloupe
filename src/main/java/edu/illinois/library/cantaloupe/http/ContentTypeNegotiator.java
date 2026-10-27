package edu.illinois.library.cantaloupe.http;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for HTTP content type negotiation based on the Accept header.
 */
public class ContentTypeNegotiator {

    private final Headers headers;

    /**
     * @param headers HTTP headers containing the Accept header for negotiation.
     */
    public ContentTypeNegotiator(Headers headers) {
        this.headers = headers;
    }

    /**
     * @param limitToTypes Media types to limit the result to, in order of most
     *                     to least preferred by the application.
     * @return             Best media type conforming to client preferences as
     *                     expressed in the {@code Accept} header; or {@code
     *                     null} if negotiation failed.
     */
    public String negotiateContentType(List<String> limitToTypes) {
        return getPreferredMediaTypes().stream()
                .filter(limitToTypes::contains)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return List of client-preferred media types as expressed in the
     *         {@code Accept} request header.
     * @see    <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">
     *         RFC 2616</a>
     */
    public List<String> getPreferredMediaTypes() {
        class Preference implements Comparable<Preference> {
            private String mediaType;
            private float qValue;

            @Override
            public int compareTo(Preference o) {
                if (o.qValue < qValue) {
                    return -1;
                } else if (o.qValue > qValue) {
                    return 1;
                }
                return 0;
            }
        }

        final List<Preference> preferences = new ArrayList<>();
        final String acceptHeader = headers.getFirstValue("Accept");
        if (acceptHeader != null) {
            String[] clauses = acceptHeader.split(",");
            for (String clause : clauses) {
                String[] parts        = clause.split(";");
                Preference preference = new Preference();
                preference.mediaType  = parts[0].trim();
                if ("*/*".equals(preference.mediaType)) {
                    continue;
                }
                if (parts.length > 1) {
                    String q = parts[1].trim();
                    if (q.startsWith("q=")) {
                        q = q.substring(2);
                        preference.qValue = Float.parseFloat(q);
                    }
                } else {
                    preference.qValue = 1;
                }
                preferences.add(preference);
            }
        }
        return preferences.stream()
                .sorted()
                .map(p -> p.mediaType)
                .collect(Collectors.toUnmodifiableList());
    }
}
