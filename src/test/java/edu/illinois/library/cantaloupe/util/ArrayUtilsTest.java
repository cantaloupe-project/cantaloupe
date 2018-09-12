package edu.illinois.library.cantaloupe.util;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class ArrayUtilsTest {

    @Test
    public void testMergeWithOneArray() {
        List<byte[]> arrays = new LinkedList<>();
        arrays.add(new byte[] { 0x32 });

        assertArrayEquals(new byte[] { 0x32 }, ArrayUtils.merge(arrays));
    }

    @Test
    public void testMergeWithMultipleArrays() {
        List<byte[]> arrays = new LinkedList<>();
        arrays.add(new byte[] { 0x32, 0x38 });
        arrays.add(new byte[] { 0x1f });

        assertArrayEquals(new byte[] { 0x32, 0x38, 0x1f },
                ArrayUtils.merge(arrays));
    }

}