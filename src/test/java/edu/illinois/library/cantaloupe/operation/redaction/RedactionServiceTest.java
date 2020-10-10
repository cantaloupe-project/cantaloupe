package edu.illinois.library.cantaloupe.operation.redaction;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Rectangle;
import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RedactionServiceTest extends BaseTest {

    private RedactionService instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new RedactionService();
    }

    @Test
    void testRedactionsForWithRedactions() throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("redacted"));

        List<Redaction> redactions = instance.redactionsFor(proxy);
        assertEquals(1, redactions.size());
        assertEquals(new Rectangle(0, 10, 50, 70),
                redactions.get(0).getRegion());
    }

    @Test
    void testRedactionsForWithNoRedactions() throws Exception {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        proxy.getRequestContext().setIdentifier(new Identifier("bogus"));

        List<Redaction> redactions = instance.redactionsFor(proxy);
        assertTrue(redactions.isEmpty());
    }

}
