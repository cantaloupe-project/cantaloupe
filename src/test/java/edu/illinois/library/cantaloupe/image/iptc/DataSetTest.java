package edu.illinois.library.cantaloupe.image.iptc;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DataSetTest extends BaseTest {

    private DataSet instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x42 });
    }

    @Test
    void testJSONSerialization() throws Exception {
        String expected = String.format(
                "{\"record\":%d,\"tag\":%d,\"dataField\":\"I0I=\"}",
                Tag.FILE_VERSION.getRecord().getRecordNum(),
                Tag.FILE_VERSION.getDataSetNum());
        String actual = new ObjectMapper().writer()
                .writeValueAsString(instance);
        assertEquals(expected, actual);
    }

    @Test
    void testJSONDeserialization() throws Exception {
        String json = String.format(
                "{\"record\":%d,\"tag\":%d,\"dataField\":\"I0I=\"}",
                Tag.FILE_VERSION.getRecord().getRecordNum(),
                Tag.FILE_VERSION.getDataSetNum());
        DataSet actual = new ObjectMapper().readValue(json, DataSet.class);
        assertEquals(instance, actual);
    }

    /* equals() */

    @Test
    void testEqualsWithEqualInstances() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x42 });
        assertEquals(instance, instance2);
    }

    @Test
    void testEqualsWithUnequalTags() {
        DataSet instance2 = new DataSet(Tag.CREDIT, new byte[] { 0x23, 0x42 });
        assertNotEquals(instance, instance2);
    }

    @Test
    void testEqualsWithUnequalDataFields() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x12 });
        assertNotEquals(instance, instance2);
    }

    /* getDataFieldAsLong() */

    @Test
    void testGetDataFieldAsLongWithIntegerType() {
        DataSet instance = new DataSet(Tag.FILE_VERSION,
                ByteBuffer.allocate(4).putInt(54352643).array());
        assertEquals(54352643, instance.getDataFieldAsLong());
    }

    @Test
    void testGetDataFieldAsLongWithDigitsType() {
        DataSet instance = new DataSet(Tag.URGENCY, new byte[] { 3 });
        assertEquals(3, instance.getDataFieldAsLong());
    }

    /* getDataFieldAsString() */

    @Test
    void testGetDataFieldAsStringWithIntegerType() {
        DataSet instance = new DataSet(Tag.FILE_VERSION,
                ByteBuffer.allocate(4).putInt(54352643).array());
        assertEquals("54352643", instance.getDataFieldAsString());
    }

    @Test
    void testGetDataFieldAsStringWithStringType() {
        DataSet instance = new DataSet(
                Tag.CITY, "Urbana".getBytes(StandardCharsets.US_ASCII));
        assertEquals("Urbana", instance.getDataFieldAsString());
    }

    @Test
    void testGetDataFieldAsStringWithDigitsType() {
        DataSet instance = new DataSet(Tag.URGENCY, new byte[] {
                0x32, 0x30, 0x30, 0x30, 0x30, 0x31, 0x30, 0x31 });
        assertEquals("20000101", instance.getDataFieldAsString());
    }

    /* hashCode() */

    @Test
    void testHashCodeWithEqualInstances() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x42 });
        assertEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void testHashCodeWithUnequalTags() {
        DataSet instance2 = new DataSet(Tag.CREDIT, new byte[] { 0x23, 0x42 });
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    @Test
    void testHashCodeWithUnequalDataFields() {
        DataSet instance2 = new DataSet(Tag.FILE_VERSION, new byte[] { 0x23, 0x12 });
        assertNotEquals(instance.hashCode(), instance2.hashCode());
    }

    /* toMap() */

    @Test
    void testToMap() {
        Map<String,Object> expected = Map.of(
                Tag.FILE_VERSION.getName(),
                instance.getDataFieldAsLong());
        assertEquals(expected, instance.toMap());
    }

}
