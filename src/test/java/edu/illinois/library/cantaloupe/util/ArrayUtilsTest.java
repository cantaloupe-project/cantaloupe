package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArrayUtilsTest extends BaseTest {

    @Test
    void testChunkifyWithSingleChunk() {
        byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04 };
        List<byte[]> chunks = ArrayUtils.chunkify(data, 5);
        assertEquals(1, chunks.size());
        assertArrayEquals(data, chunks.get(0));
    }

    @Test
    void testChunkifyWithMultipleChunks() {
        byte[] data = { 0x00, 0x01, 0x02, 0x03, 0x04 };
        List<byte[]> chunks = ArrayUtils.chunkify(data, 2);
        assertEquals(3, chunks.size());
        assertArrayEquals(new byte[] { 0x00, 0x01 }, chunks.get(0));
        assertArrayEquals(new byte[] { 0x02, 0x03 }, chunks.get(1));
        assertArrayEquals(new byte[] { 0x04 }, chunks.get(2));
    }

    @Test
    void testChunkifyWithEmptyArgument() {
        byte[] data = {};
        List<byte[]> chunks = ArrayUtils.chunkify(data, 5);
        assertEquals(1, chunks.size());
        assertArrayEquals(data, chunks.get(0));
    }

    @Test
    void testMergeWithOneArray() {
        List<byte[]> arrays = new LinkedList<>();
        arrays.add(new byte[] { 0x32 });

        assertArrayEquals(new byte[] { 0x32 }, ArrayUtils.merge(arrays));
    }

    @Test
    void testMergeWithMultipleArrays() {
        List<byte[]> arrays = new LinkedList<>();
        arrays.add(new byte[] { 0x32, 0x38 });
        arrays.add(new byte[] { 0x1f });

        assertArrayEquals(new byte[] { 0x32, 0x38, 0x1f },
                ArrayUtils.merge(arrays));
    }

}