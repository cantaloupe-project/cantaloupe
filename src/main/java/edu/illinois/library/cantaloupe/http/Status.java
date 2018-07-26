package edu.illinois.library.cantaloupe.http;

/**
 * @see <a href="https://www.iana.org/assignments/http-status-codes/http-status-codes.xhtml">
 *     HTTP Status Code Registry</a>
 */
public final class Status {

    public static final Status CONTINUE                        = new Status(100);
    public static final Status SWITCHING_PROTOCOLS             = new Status(101);
    public static final Status PROCESSING                      = new Status(102);
    public static final Status EARLY_HINTS                     = new Status(103);
    public static final Status OK                              = new Status(200);
    public static final Status CREATED                         = new Status(201);
    public static final Status ACCEPTED                        = new Status(202);
    public static final Status NON_AUTHORITATIVE_INFORMATION   = new Status(203);
    public static final Status NO_CONTENT                      = new Status(204);
    public static final Status RESET_CONTENT                   = new Status(205);
    public static final Status PARTIAL_CONTENT                 = new Status(206);
    public static final Status MULTI_STATUS                    = new Status(207);
    public static final Status ALREADY_REPORTED                = new Status(208);
    public static final Status IM_USED                         = new Status(226);
    public static final Status MULTIPLE_CHOICES                = new Status(300);
    public static final Status MOVED_PERMANENTLY               = new Status(301);
    public static final Status FOUND                           = new Status(302);
    public static final Status SEE_OTHER                       = new Status(303);
    public static final Status NOT_MODIFIED                    = new Status(304);
    public static final Status USE_PROXY                       = new Status(305);
    public static final Status UNUSED                          = new Status(306);
    public static final Status TEMPORARY_REDIRECT              = new Status(307);
    public static final Status PERMANENT_REDIRECT              = new Status(308);
    public static final Status BAD_REQUEST                     = new Status(400);
    public static final Status UNAUTHORIZED                    = new Status(401);
    public static final Status PAYMENT_REQUIRED                = new Status(402);
    public static final Status FORBIDDEN                       = new Status(403);
    public static final Status NOT_FOUND                       = new Status(404);
    public static final Status METHOD_NOT_ALLOWED              = new Status(405);
    public static final Status NOT_ACCEPTABLE                  = new Status(406);
    public static final Status PROXY_AUTHENTICATION_REQUIRED   = new Status(407);
    public static final Status REQUEST_TIMEOUT                 = new Status(408);
    public static final Status CONFLICT                        = new Status(409);
    public static final Status GONE                            = new Status(410);
    public static final Status LENGTH_REQUIRED                 = new Status(411);
    public static final Status PRECONDITION_FAILED             = new Status(412);
    public static final Status PAYLOAD_TOO_LARGE               = new Status(413);
    public static final Status URI_TOO_LONG                    = new Status(414);
    public static final Status UNSUPPORTED_MEDIA_TYPE          = new Status(415);
    public static final Status RANGE_NOT_SATISFIABLE           = new Status(416);
    public static final Status EXPECTATION_FAILED              = new Status(417);
    public static final Status MISDIRECTED_REQUEST             = new Status(421);
    public static final Status UNPROCESSABLE_ENTITY            = new Status(422);
    public static final Status LOCKED                          = new Status(423);
    public static final Status FAILED_DEPENDENCY               = new Status(424);
    public static final Status TOO_EARLY                       = new Status(425);
    public static final Status UPGRADE_REQUIRED                = new Status(426);
    public static final Status PRECONDITION_REQUIRED           = new Status(428);
    public static final Status TOO_MANY_REQUESTS               = new Status(429);
    public static final Status REQUEST_HEADER_FIELDS_TOO_LARGE = new Status(431);
    public static final Status UNAVAILABLE_FOR_LEGAL_REASONS   = new Status(451);
    public static final Status INTERNAL_SERVER_ERROR           = new Status(500);
    public static final Status NOT_IMPLEMENTED                 = new Status(501);
    public static final Status BAD_GATEWAY                     = new Status(502);
    public static final Status SERVICE_UNAVAILABLE             = new Status(503);
    public static final Status GATEWAY_TIMEOUT                 = new Status(504);
    public static final Status HTTP_VERSION_NOT_SUPPORTED      = new Status(505);
    public static final Status VARIANT_ALSO_NEGOTIATES         = new Status(506);
    public static final Status INSUFFICIENT_STORAGE            = new Status(507);
    public static final Status LOOP_DETECTED                   = new Status(508);
    public static final Status NOT_EXTENDED                    = new Status(510);
    public static final Status NETWORK_AUTHENTICATION_REQUIRED = new Status(511);

    private int code;
    private String description;

    public Status(int code) {
        this.code = code;
        switch (code) {
            case 100:
                description = "Continue";
                break;
            case 101:
                description = "Switching Protocols";
                break;
            case 102:
                description = "Processing";
                break;
            case 103:
                description = "Early Hints";
                break;
            case 200:
                description = "OK";
                break;
            case 201:
                description = "Created";
                break;
            case 202:
                description = "Accepted";
                break;
            case 203:
                description = "Non-Authoritative Information";
                break;
            case 204:
                description = "No Content";
                break;
            case 205:
                description = "Reset Content";
                break;
            case 206:
                description = "Partial Content";
                break;
            case 207:
                description = "Multi-Status";
                break;
            case 208:
                description = "Already Reported";
                break;
            case 226:
                description = "IM Used";
                break;
            case 300:
                description = "Multiple Choices";
                break;
            case 301:
                description = "Moved Permanently";
                break;
            case 302:
                description = "Found";
                break;
            case 303:
                description = "See Other";
                break;
            case 304:
                description = "Not Modified";
                break;
            case 305:
                description = "Use Proxy";
                break;
            case 307:
                description = "Temporary Redirect";
                break;
            case 308:
                description = "Permanent Redirect";
                break;
            case 400:
                description = "Bad Request";
                break;
            case 401:
                description = "Unauthorized";
                break;
            case 402:
                description = "Payment Required";
                break;
            case 403:
                description = "Forbidden";
                break;
            case 404:
                description = "Not Found";
                break;
            case 405:
                description = "Method Not Allowed";
                break;
            case 406:
                description = "Not Acceptable";
                break;
            case 407:
                description = "Proxy Authentication Required";
                break;
            case 408:
                description = "Request Timeout";
                break;
            case 409:
                description = "Conflict";
                break;
            case 410:
                description = "Gone";
                break;
            case 411:
                description = "Length Required";
                break;
            case 412:
                description = "Precondition Failed";
                break;
            case 413:
                description = "Payload Too Large";
                break;
            case 414:
                description = "URI Too Long";
                break;
            case 415:
                description = "Unsupported Media Type";
                break;
            case 416:
                description = "Range Not Satisfiable";
                break;
            case 417:
                description = "Expectation Failed";
                break;
            case 421:
                description = "Misdirected Request";
                break;
            case 422:
                description = "Unprocessable Entity";
                break;
            case 423:
                description = "Locked";
                break;
            case 424:
                description = "Failed Dependency";
                break;
            case 425:
                description = "Too Early";
                break;
            case 426:
                description = "Upgrade Required";
                break;
            case 428:
                description = "Precondition Required";
                break;
            case 429:
                description = "Too Many Requests";
                break;
            case 431:
                description = "Request Header Fields Too Large";
                break;
            case 451:
                description = "Unavailable For Legal Reasons";
                break;
            case 500:
                description = "Internal Server Error";
                break;
            case 501:
                description = "Not Implemented";
                break;
            case 502:
                description = "Bad Gateway";
                break;
            case 503:
                description = "Service Unavailable";
                break;
            case 504:
                description = "Gateway Timeout";
                break;
            case 505:
                description = "HTTP Version Not Supported";
                break;
            case 506:
                description = "Variant Also Negotiates";
                break;
            case 507:
                description = "Insufficient Storage";
                break;
            case 508:
                description = "Loop Detected";
                break;
            case 510:
                description = "Not Extended";
                break;
            case 511:
                description = "Network Authentication Required";
                break;
            default:
                description = "Unknown";
                break;
        }
    }

    public Status(int code, String description) {
        this(code);
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof Status) {
            Status other = (Status) obj;
            return getCode() == other.getCode();
        }
        return super.equals(obj);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getCode());
    }

    @Override
    public String toString() {
        return code + " " + description;
    }

}
