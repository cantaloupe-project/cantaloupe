package edu.illinois.library.cantaloupe.auth;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizerFactoryTest extends BaseTest {

    private AuthorizerFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new AuthorizerFactory();
    }

    @Test
    void testNewAuthorizerWithNoArguments() {
        assertTrue(instance.newAuthorizer() instanceof PermissiveAuthorizer);
    }

    @Test
    void testNewAuthorizerWithNullArguments() {
        assertTrue(instance.newAuthorizer(null, null) instanceof PermissiveAuthorizer);
    }

    @Test
    void testNewAuthorizerWithArgument() {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(
                new Identifier("forbidden-code-no-reason.jpg"));

        assertTrue(instance.newAuthorizer(proxy) instanceof DelegateAuthorizer);
    }

}
