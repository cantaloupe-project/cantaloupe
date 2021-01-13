package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.delegate.DelegateProxy;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.resource.iiif.FormatException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class ParametersTest extends BaseTest {

    private Parameters instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Parameters(
                "identifier", "0,0,200,200", "pct:50", "5", "native", "jpg");
    }

    @Test
    void testFromUri() {
        Parameters params =
                Parameters.fromUri("bla/20,20,50,50/pct:90/15/native.jpg");
        assertEquals("bla", params.getIdentifier());
        assertEquals("20,20,50,50", params.getRegion().toString());
        assertEquals(90f, params.getSize().getPercent(), 0.0000001f);
        assertEquals(15f, params.getRotation().getDegrees(), 0.0000001f);
        assertEquals(Quality.NATIVE, params.getQuality());
        assertEquals(Format.get("jpg"), params.getOutputFormat());
    }

    @Test
    void testFromUriWithInvalidURI1() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Parameters.fromUri("bla/20,20,50,50/15/bitonal.jpg"));
    }

    @Test
    void testFromUriWithInvalidURI2() {
        assertThrows(IllegalClientArgumentException.class,
                () -> Parameters.fromUri("bla/20,20,50,50/pct:90/15/bitonal"));
    }

    @Test
    void testConstructorWithInvalidQuality() {
        assertThrows(IllegalClientArgumentException.class,
                () -> new Parameters(
                        "identifier", "0,0,200,200", "pct:50", "5", "bogus", "jpg"));
    }

    @Test
    void testConstructorWithUnsupportedOutputFormat() {
        assertThrows(FormatException.class,
                () -> new Parameters(
                        "identifier", "0,0,200,200", "pct:50", "5", "native", "bogus"));
    }

    @Test
    void testToOperationList() {
        DelegateProxy proxy = TestUtil.newDelegateProxy();
        OperationList opList = instance.toOperationList(proxy);

        assertEquals(instance.getIdentifier(),
                opList.getMetaIdentifier().getIdentifier().toString());
        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Crop);
        assertTrue(it.next() instanceof Scale);
        assertTrue(it.next() instanceof Rotate);
    }

    @Test
    void testToOperationListOmitsCropIfRegionIsFull() {
        DelegateProxy delegateProxy = TestUtil.newDelegateProxy();
        instance = new Parameters(
                "identifier", "full", "pct:50", "5", "native", "jpg");
        OperationList opList = instance.toOperationList(delegateProxy);

        assertEquals(instance.getIdentifier(),
                opList.getMetaIdentifier().getIdentifier().toString());
        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Scale);
        assertTrue(it.next() instanceof Rotate);
    }

    @Test
    void testToOperationListOmitsScaleIfSizeIsFull() {
        DelegateProxy delegateProxy = TestUtil.newDelegateProxy();
        instance = new Parameters(
                "identifier", "0,0,30,30", "full", "5", "native", "jpg");
        OperationList opList = instance.toOperationList(delegateProxy);

        assertEquals(instance.getIdentifier(),
                opList.getMetaIdentifier().getIdentifier().toString());
        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Crop);
        assertTrue(it.next() instanceof Rotate);
    }

    @Test
    void testToOperationListOmitsRotateIfRotationIsZero() {
        DelegateProxy delegateProxy = TestUtil.newDelegateProxy();
        instance = new Parameters(
                "identifier", "0,0,30,30", "full", "0", "native", "jpg");
        OperationList opList = instance.toOperationList(delegateProxy);

        assertEquals(instance.getIdentifier(),
                opList.getMetaIdentifier().getIdentifier().toString());
        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Crop);
        assertTrue(it.next() instanceof Encode);
    }

}
