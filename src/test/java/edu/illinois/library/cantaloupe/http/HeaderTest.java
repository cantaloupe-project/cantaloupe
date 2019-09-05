package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HeaderTest extends BaseTest {

    private Header instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Header("name", "value");
    }

    @Test
    void testConstructor() {
        assertEquals("name", instance.getName());
        assertEquals("value", instance.getValue());
    }

    @Test
    void testCopyConstructor() {
        Header other = new Header(instance);
        assertEquals("name", other.getName());
        assertEquals("value", other.getValue());
    }

    @Test
    void testConstructorWithNullNameArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Header(null, "value"));
    }

    @Test
    void testConstructorWithNullValueArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Header("name", null));
    }

    @Test
    void testConstructorWithZeroLengthNameArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Header("", "value"));
    }

    @Test
    void testConstructorWithZeroLengthValueArgument() {
        instance = new Header("name", "");
        assertEquals("", instance.getValue());
    }

    @Test
    void testEqualsWhenEqual() {
        Header h1 = new Header("1", "2");
        Header h2 = new Header("1", "2");
        assertEquals(h1, h2);
    }

    @Test
    void testEqualsWithDifferentNames() {
        Header h1 = new Header("1", "2");
        Header h2 = new Header("3", "2");
        assertNotEquals(h1, h2);
    }

    @Test
    void testEqualsWithDifferentValues() {
        Header h1 = new Header("1", "5");
        Header h2 = new Header("1", "2");
        assertNotEquals(h1, h2);
    }

    @Test
    void testEqualsWithDifferentClasses() {
        Header h1 = new Header("1", "2");
        assertNotEquals(h1, "1: 2");
    }

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    void testToString() {
        assertEquals(instance.getName() + ": " + instance.getValue(),
                instance.toString());
    }

}
