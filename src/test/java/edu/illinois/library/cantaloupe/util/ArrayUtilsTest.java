package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ArrayUtilsTest extends BaseTest {

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