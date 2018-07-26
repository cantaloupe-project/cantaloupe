package edu.illinois.library.cantaloupe.auth;

public interface CredentialStore {

    String getSecret(String user);

}
