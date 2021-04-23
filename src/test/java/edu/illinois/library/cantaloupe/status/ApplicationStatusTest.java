package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationStatusTest extends BaseTest {

    private ApplicationStatus instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ApplicationStatus();
    }

    @Test
    void getInfoCacheMaxSize() {
        assertTrue(instance.getInfoCacheMaxSize() > 100);
    }

    @Test
    void getInfoCacheSize() {
        assertEquals(0, instance.getInfoCacheSize());
    }

    @Test
    void getNumProcessors() {
        assertTrue(instance.getNumProcessors() >= 1);
    }

    @Test
    void getVMFreeHeap() {
        assertTrue(instance.getVMFreeHeap() > 1000);
    }

    @Test
    void getVMInfo() {
        assertNotNull(instance.getVMInfo());
    }

    @Test
    void getVMMaxHeap() {
        assertTrue(instance.getVMMaxHeap() > 1000);
    }

    @Test
    void getVMName() {
        assertNotNull(instance.getVMName());
    }

    @Test
    void getVMTotalHeap() {
        assertTrue(instance.getVMTotalHeap() > 1000);
    }

    @Test
    void getVMUptime() {
        assertTrue(instance.getVMUptime() > 10);
    }

    @Test
    void getVMUsedHeap() {
        assertTrue(instance.getVMUsedHeap() > 1000);
    }

    @Test
    void getVMVendor() {
        assertNotNull(instance.getVMVendor());
    }

    @Test
    void getVMVersion() {
        assertNotNull(instance.getVMVersion());
    }

    @Test
    void toMap() {
        Map<String,Object> map = instance.toMap();
        assertEquals(3, map.size());
    }

}