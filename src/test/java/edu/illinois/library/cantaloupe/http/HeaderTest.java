package edu.illinois.library.cantaloupe.http;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HeaderTest {

    private Header instance;

    @Before
    public void setUp() throws Exception {
        instance = new Header("name", "value");
    }

    @Test
    public void testConstructor() {
        assertEquals("name", instance.getName());
        assertEquals("value", instance.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullNameArgument() {
        new Header(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullValueArgument() {
        new Header("name", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZeroLengthNameArgument() {
        new Header("", "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZeroLengthValueArgument() {
        new Header("name", "");
    }

    @Test
    public void testEqualsWhenEqual() {
        Header h1 = new Header("1", "2");
        Header h2 = new Header("1", "2");
        assertEquals(h1, h2);
    }

    @Test
    public void testEqualsWithDifferentNames() {
        Header h1 = new Header("1", "2");
        Header h2 = new Header("3", "2");
        assertNotEquals(h1, h2);
    }

    @Test
    public void testEqualsWithDifferentValues() {
        Header h1 = new Header("1", "5");
        Header h2 = new Header("1", "2");
        assertNotEquals(h1, h2);
    }

    @Test
    public void testEqualsWithDifferentClasses() {
        Header h1 = new Header("1", "2");
        assertNotEquals(h1, "1: 2");
    }

    @Test
    public void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(instance.getValue(), instance.toString());
    }

}
