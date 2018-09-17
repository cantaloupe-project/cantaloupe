package edu.illinois.library.cantaloupe;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ApplicationStatusTest {

    private ApplicationStatus instance;

    @Before
    public void setUp() throws Exception {
        instance = new ApplicationStatus();
    }

    @Test
    public void testGetDMICMaxSize() {
        assertTrue(instance.getDMICMaxSize() > 100);
    }

    @Test
    public void testGetDMICSize() {
        assertEquals(0, instance.getDMICSize());
    }

    @Test
    public void testGetInfoCacheMaxSize() {
        assertTrue(instance.getInfoCacheMaxSize() > 100);
    }

    @Test
    public void testGetInfoCacheSize() {
        assertEquals(0, instance.getInfoCacheSize());
    }

    @Test
    public void testGetVMFreeHeap() {
        assertTrue(instance.getVMFreeHeap() > 1000);
    }

    @Test
    public void testGetVMMaxHeap() {
        assertTrue(instance.getVMMaxHeap() > 1000);
    }

    @Test
    public void testGetVMTotalHeap() {
        assertTrue(instance.getVMTotalHeap() > 1000);
    }

    @Test
    public void testGetVMUptime() {
        assertTrue(instance.getVMUptime() > 10);
    }

    @Test
    public void testGetVMUsedHeap() {
        assertTrue(instance.getVMUsedHeap() > 1000);
    }

}