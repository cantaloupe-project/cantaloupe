package edu.illinois.library.cantaloupe.auth;

public class AuthorizerFactory {

    /**
     * @param args Arguments to pass to the instance's constructor.
     * @return     New instance. If {@literal args} is empty, the instance
     *             will be maximally permissive.
     */
    public Authorizer newAuthorizer(Object... args) {
        if (args.length > 0 && args[0] != null) {
            return new DelegateAuthorizer(args);
        }
        return new PermissiveAuthorizer();
    }

}
