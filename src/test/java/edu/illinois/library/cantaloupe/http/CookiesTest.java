package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CookiesTest extends BaseTest {

    private Cookies instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Cookies();
    }

    @Test
    void testFromHeaderValue() {
        Cookies expected = new Cookies();
        expected.add("name1", "value1");
        expected.add("name2", "value2");
        Cookies actual = Cookies.fromHeaderValue("name1=value1;name2=value2");
        assertEquals(expected, actual);
    }

    @Test
    void testFromHeaderValueWithNoName() {
        Cookies expected = new Cookies();
        expected.add("", "value1");
        Cookies actual = Cookies.fromHeaderValue("value1");
        assertEquals(expected, actual);
    }

    @Test
    void testFromHeaderValuePermissiveness() {
        final String torture = "abcABC012 !#$%&'()*+-./:<>?@[]^_`{|}~ 简体中文网页";

        Cookies expected = new Cookies();
        expected.add("name1", torture);
        expected.add("name2", torture);
        Cookies actual = Cookies.fromHeaderValue(
                "name1=" + torture + "; name2=" + torture);
        assertEquals(expected, actual);
    }

    @Test
    void testCopyConstructor() {
        instance.add("cookie1", "cats");
        instance.add("cookie1", "dogs");
        instance.add("cookie2", "foxes");

        Cookies other = new Cookies(instance);
        assertEquals(3, other.size());
    }

    @Test
    void testAddWithCookie() {
        assertEquals(0, instance.size());
        instance.add(new Cookie("name", "value"));
        assertEquals(1, instance.size());
    }

    @Test
    void testAddWithStrings() {
        assertEquals(0, instance.size());
        instance.add("name", "value");
        assertEquals(1, instance.size());
    }

    @Test
    void testAddAll() {
        instance.add("name", "value");
        assertEquals(1, instance.size());

        Cookies other = new Cookies();
        other.add("name2", "value2");
        other.add("name3", "value3");

        instance.addAll(other);
        assertEquals(3, instance.size());
    }

    @Test
    void testClear() {
        instance.add("name", "value");
        instance.add("name", "value");
        instance.clear();
        assertEquals(0, instance.size());
    }

    @Test
    void testEqualsWithEqualObjects() {
        Cookies h1 = new Cookies();
        Cookies h2 = new Cookies();
        assertEquals(h1, h2);

        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test", "cats2");
        assertEquals(h1, h2);
    }

    @Test
    void testEqualsWithUnequalObjects() {
        Cookies h1 = new Cookies();
        Cookies h2 = new Cookies();
        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test2", "cats2");
        assertNotEquals(h1, h2);
    }

    @Test
    void testGetAll() {
        assertEquals(0, instance.getAll().size());

        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.getAll().size());
    }

    @Test
    void testGetAllWithString() {
        assertEquals(0, instance.getAll("name").size());

        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.getAll("name").size());
    }

    @Test
    void testGetFirstValue() {
        assertNull(instance.getFirstValue("name"));

        instance.add("name", "value1");
        instance.add("name", "value2");
        assertEquals("value1", instance.getFirstValue("name"));
    }

    @Test
    void testGetFirstValueWithDefaultValue() {
        assertEquals("default value",
                instance.getFirstValue("name", "default value"));

        instance.add("name", "value1");
        instance.add("name", "value2");
        assertEquals("value1", instance.getFirstValue("name"));
    }

    @Test
    void testHashCodeWithEqualObjects() {
        Cookies h1 = new Cookies();
        Cookies h2 = new Cookies();
        assertEquals(h1.hashCode(), h2.hashCode());

        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test", "cats2");
        assertEquals(h1.hashCode(), h2.hashCode());
    }

    @Test
    void testHashCodeWithUnequalObjects() {
        Cookies h1 = new Cookies();
        Cookies h2 = new Cookies();
        h1.add("test", "cats1");
        h1.add("test", "cats2");
        h2.add("test", "cats1");
        h2.add("test2", "cats2");
        assertNotEquals(h1.hashCode(), h2.hashCode());
    }

    @Test
    void testIterator() {
        instance.add("name", "value1");
        instance.add("name", "value2");

        Iterator<Cookie> it = instance.iterator();
        assertTrue(it.hasNext());
        it.next();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    void testRemoveAll() {
        instance.add("name1", "value1");
        instance.add("name1", "value2");
        instance.add("name2", "value1");
        instance.removeAll("name1");
        assertEquals(1, instance.size());
        assertNotNull(instance.getFirstValue("name2"));
    }

    @Test
    void testSet() {
        instance.set("name1", "value1");
        instance.set("name2", "value1");
        instance.set("name2", "value2");
        assertEquals(1, instance.getAll("name2").size());
        assertEquals("value2", instance.getFirstValue("name2"));
    }

    @Test
    void testSize() {
        assertEquals(0, instance.size());
        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.size());
    }

    @Test
    void testStream() {
        instance.add("name", "value");
        instance.add("name", "value");
        assertEquals(2, instance.stream().count());
    }

    @Test
    void testToMap() {
        instance.add("name1", "value1");
        instance.add("name1", "value2");
        instance.add("name2", "value1");
        Map<String, String> map = instance.toMap();
        assertEquals(2, map.size());
    }

    @Test
    void testToStringWithNoCookies() {
        assertEquals("", instance.toString());
    }

    @Test
    void testToStringWithOneCookie() {
        instance.add("cats", "yes");
        assertEquals("cats=yes", instance.toString());
    }

    @Test
    void testToStringWithMultipleCookies() {
        instance.add("cats", "yes");
        instance.add("dogs", "yes");
        assertEquals("cats=yes; dogs=yes", instance.toString());
    }

    @Test
    void testToStringWithEmptyCookieName() {
        instance.add("", "yes");
        assertEquals("yes", instance.toString());
    }

    @Test
    void testToStringWithEmptyCookieValue() {
        instance.add("key", "");
        assertEquals("key=", instance.toString());
    }

}
