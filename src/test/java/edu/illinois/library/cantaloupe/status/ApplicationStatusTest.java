package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationStatusTest extends BaseTest {

    private ApplicationStatus instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ApplicationStatus();
    }

    @Test
    void testGetDMICMaxSize() {
        assertTrue(instance.getDMICMaxSize() > 100);
    }

    @Test
    void testGetDMICSize() {
        assertEquals(0, instance.getDMICSize());
    }

    @Test
    void testGetInfoCacheMaxSize() {
        assertTrue(instance.getInfoCacheMaxSize() > 100);
    }

    @Test
    void testGetInfoCacheSize() {
        assertEquals(0, instance.getInfoCacheSize());
    }

    @Test
    void testGetVMFreeHeap() {
        assertTrue(instance.getVMFreeHeap() > 1000);
    }

    @Test
    void testGetVMMaxHeap() {
        assertTrue(instance.getVMMaxHeap() > 1000);
    }

    @Test
    void testGetVMTotalHeap() {
        assertTrue(instance.getVMTotalHeap() > 1000);
    }

    @Test
    void testGetVMUptime() {
        assertTrue(instance.getVMUptime() > 10);
    }

    @Test
    void testGetVMUsedHeap() {
        assertTrue(instance.getVMUsedHeap() > 1000);
    }

}