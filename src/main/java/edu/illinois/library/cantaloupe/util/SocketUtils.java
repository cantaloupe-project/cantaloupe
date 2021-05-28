package edu.illinois.library.cantaloupe.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public final class SocketUtils {

    public static int getOpenPort() {
        return SocketUtils.getOpenPorts(1)[0];
    }

    public static int[] getOpenPorts(int howMany) {
        final List<Integer> ports = new ArrayList<>(65535);
        int numFound = 0;
        for (int port = 1; port < 65535; port++) {
            if (numFound >= howMany) {
                break;
            }
            try (ServerSocket socket = new ServerSocket(port)) {
                ports.add(port);
                numFound++;
            } catch (IOException ignore) {
                // probably not open, fine
            }
        }
        return ports.stream().mapToInt(i -> i).toArray();
    }

        for (ServerSocket socket : triedSockets) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("TestUtil.getOpenPort(): " + e.getMessage());
            }
        }

        return ports;
    }

    private SocketUtils() {}

}
