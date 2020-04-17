package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class CookieTest extends BaseTest {

    private Cookie instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Cookie("name", "value");
    }

    @Test
    void testConstructor() {
        assertEquals("name", instance.getName());
        assertEquals("value", instance.getValue());
    }

    @Test
    void testCopyConstructor() {
        Cookie other = new Cookie(instance);
        assertEquals("name", other.getName());
        assertEquals("value", other.getValue());
    }

    @Test
    void testConstructorWithNullNameArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cookie(null, "value"));
    }

    @Test
    void testConstructorWithNullValueArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cookie("name", null));
    }

    @Test
    void testConstructorWithZeroLengthNameArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cookie("", "value"));
    }

    @Test
    void testConstructorWithZeroLengthValueArgument() {
        instance = new Cookie("name", "");
        assertEquals("", instance.getValue());
    }

    @Test
    void testEqualsWhenEqual() {
        Cookie h1 = new Cookie("1", "2");
        Cookie h2 = new Cookie("1", "2");
        assertEquals(h1, h2);
    }

    @Test
    void testEqualsWithDifferentNames() {
        Cookie h1 = new Cookie("1", "2");
        Cookie h2 = new Cookie("3", "2");
        assertNotEquals(h1, h2);
    }

    @Test
    void testEqualsWithDifferentValues() {
        Cookie h1 = new Cookie("1", "5");
        Cookie h2 = new Cookie("1", "2");
        assertNotEquals(h1, h2);
    }

    @Test
    void testEqualsWithDifferentClasses() {
        Cookie h1 = new Cookie("1", "2");
        assertNotEquals(h1, "1: 2");
    }

    @Test
    void testHashCode() {
        int expected = Arrays.hashCode(
                new String[] { instance.getName(), instance.getValue() });
        assertEquals(expected, instance.hashCode());
    }

    @Test
    void testToString() {
        assertEquals(instance.getName() + "=" + instance.getValue(),
                instance.toString());
    }

}
