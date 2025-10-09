package edu.illinois.library.cantaloupe.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import edu.illinois.library.cantaloupe.http.Status;
import edu.illinois.library.cantaloupe.resource.ResourceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BasicAuth {
        /**
     * Checks the {@code Authorization} header for credentials that exist in
     * the given {@link CredentialStore}. If not found, sends a {@code
     * WWW-Authenticate} header and throws an exception.
     *
     * @param realm           Basic realm.
     * @param credentialStore Credential store.
     * @throws ResourceException if authentication failed.
     */
    public static final void authenticateUsingBasic(String realm,
                                                CredentialStore credentialStore,
                                                HttpServletRequest request,
                                                HttpServletResponse response)
            throws ResourceException {
        boolean isAuthenticated = false;
        String header = request.getHeader("Authorization");
        if (header != null && "Basic ".equals(header.substring(0, Math.min(header.length(), 6)))) {
            String encoded = header.substring(6);
            String decoded = new String(Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length == 2) {
                String user = parts[0];
                String secret = parts[1];
                if (secret.equals(credentialStore.getSecret(user))) {
                    isAuthenticated = true;
                }
            }
        }
        if (!isAuthenticated) {
            response.setHeader("WWW-Authenticate",
                    "Basic realm=\"" + realm + "\" charset=\"UTF-8\"");
            throw new ResourceException(Status.UNAUTHORIZED);
        }
    }
}
