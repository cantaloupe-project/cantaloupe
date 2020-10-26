package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MetaIdentifierTransformerFactoryTest extends BaseTest {

    private MetaIdentifierTransformerFactory instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new MetaIdentifierTransformerFactory();
    }

    @Test
    void testAllImplementations() {
        Set<Class<?>> expected = Set.of(
                StandardMetaIdentifierTransformer.class,
                DelegateMetaIdentifierTransformer.class);
        assertEquals(expected,
                MetaIdentifierTransformerFactory.allImplementations());
    }

    @Test
    void testNewInstanceReturnsACorrectInstance() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DELEGATE_SCRIPT_ENABLED, true);
        config.setProperty(Key.DELEGATE_SCRIPT_PATHNAME,
                TestUtil.getFixture("delegates.rb").toString());

        DelegateProxy delegateProxy = TestUtil.newDelegateProxy();

        config.setProperty(Key.META_IDENTIFIER_TRANSFORMER,
                StandardMetaIdentifierTransformer.class.getSimpleName());
        MetaIdentifierTransformer xformer = instance.newInstance(delegateProxy);
        assertTrue(xformer instanceof StandardMetaIdentifierTransformer);

        config.setProperty(Key.META_IDENTIFIER_TRANSFORMER,
                DelegateMetaIdentifierTransformer.class.getSimpleName());
        xformer = instance.newInstance(delegateProxy);
        assertTrue(xformer instanceof DelegateMetaIdentifierTransformer);
    }

}