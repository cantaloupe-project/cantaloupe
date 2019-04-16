package edu.illinois.library.cantaloupe.image.iptc;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.*;

public class DataSetTest extends BaseTest {

    private DataSet instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x42 });
    }

    @Test
    public void testJSONSerialization() throws Exception {
        String expected = String.format(
                "{\"record\":%d,\"tag\":%d,\"dataField\":\"I0I=\"}",
                Tag.FILE_VERSION.getRecord().getRecordNum(),
                Tag.FILE_VERSION.getDataSetNum());
        String actual = new ObjectMapper().writer()
                .writeValueAsString(instance);
        assertEquals(expected, actual);
    }

    @Test
    public void testJSONDeserialization() throws Exception {
        String json = String.format(
                "{\"record\":%d,\"tag\":%d,\"dataField\":\"I0I=\"}",
                Tag.FILE_VERSION.getRecord().getRecordNum(),
                Tag.FILE_VERSION.getDataSetNum());
        DataSet actual = new ObjectMapper().readValue(json, DataSet.class);
        assertEquals(instance, actual);
    }

    /* equals() */

    @Test
    public void testEqualsWithEqualInstances() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x42 });
        assertEquals(instance, instance2);
    }

    @Test
    public void testEqualsWithUnequalTags() {
        DataSet instance2 = new DataSet(Tag.CREDIT, new byte[] { 0x23, 0x42 });
        assertNotEquals(instance, instance2);
    }

    @Test
    public void testEqualsWithUnequalDataFields() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x12 });
        assertNotEquals(instance, instance2);
    }

    /* getDataFieldAsLong() */

    @Test
    public void testGetDataFieldAsLongWithIntegerType() {
        DataSet instance = new DataSet(Tag.FILE_VERSION,
                ByteBuffer.allocate(4).putInt(54352643).array());
        assertEquals(54352643, instance.getDataFieldAsLong());
    }

    @Test
    public void testGetDataFieldAsLongWithDigitsType() {
        DataSet instance = new DataSet(Tag.URGENCY, new byte[] { 3 });
        assertEquals(3, instance.getDataFieldAsLong());
    }

    /* getDataFieldAsString() */

    @Test
    public void testGetDataFieldAsStringWithIntegerType() {
        DataSet instance = new DataSet(Tag.FILE_VERSION,
                ByteBuffer.allocate(4).putInt(54352643).array());
        assertEquals("54352643", instance.getDataFieldAsString());
    }

    @Test
    public void testGetDataFieldAsStringWithStringType() {
        DataSet instance = new DataSet(
                Tag.CITY, "Urbana".getBytes(StandardCharsets.US_ASCII));
        assertEquals("Urbana", instance.getDataFieldAsString());
    }

    @Test
    public void testGetDataFieldAsStringWithDigitsType() {
        DataSet instance = new DataSet(Tag.URGENCY, new byte[] {
                0x32, 0x30, 0x30, 0x30, 0x30, 0x31, 0x30, 0x31 });
        assertEquals("20000101", instance.getDataFieldAsString());
    }

    /* hashCode() */

    @Test
    public void testHashCodeWithEqualInstances() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x42 });
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    public void testHashCodeWithUnequalTags() {
        DataSet instance2 = new DataSet(Tag.CREDIT, new byte[] { 0x23, 0x42 });
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    public void testHashCodeWithUnequalDataFields() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x12 });
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    /* toMap() */

    @Test
    public void testToMap() {
        Map<String,Object> expected = Map.of(
                Tag.FILE_VERSION.getName(),
                instance.getDataFieldAsLong());
        assertEquals(expected, instance.toMap());
    }

}
