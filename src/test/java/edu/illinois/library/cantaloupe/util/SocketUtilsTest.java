package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

class SocketUtilsTest extends BaseTest {

    @Test
    void testGetOpenPort() throws Exception {
        ServerSocket socket = null;
        try {
            int port = SocketUtils.getOpenPort();
            socket = new ServerSocket(port);
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    @Test
    void testGetOpenPorts() throws Exception {
        int[] ports = SocketUtils.getOpenPorts(2);

        for (int port : ports) {
            ServerSocket socket = null;
            try {
                socket = new ServerSocket(port);
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        }
    }

    @Test
    void testGetUsedPort() {
        int port = SocketUtils.getUsedPort();
        assertThrows(IOException.class, () -> new ServerSocket(port));
    }

    @Test
    void testGetUsedPorts() {
        int[] ports = SocketUtils.getUsedPorts(2);
        for (int port : ports) {
            assertThrows(IOException.class, () -> new ServerSocket(port));
        }
    }

}
