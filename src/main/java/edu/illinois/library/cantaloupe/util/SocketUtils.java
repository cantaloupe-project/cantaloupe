package edu.illinois.library.cantaloupe.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SocketUtils {

    public static int getOpenPort() {
        return SocketUtils.getOpenPorts(1)[0];
    }

    public static int[] getOpenPorts(int howMany) {
        final Set<ServerSocket> triedSockets = new HashSet<>();
        final int[] ports = new int[howMany];

        for (int i = 0; i < howMany; i++) {
            try {
                ServerSocket socket = new ServerSocket(0);
                ports[i] = socket.getLocalPort();
                triedSockets.add(socket);
                // Leave it open for now so it isn't returned again on the next
                // iteration.
            } catch (IOException e) {
                System.err.println("TestUtil.getOpenPorts(): " + e.getMessage());
            }
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

    public static int getUsedPort() {
        return getUsedPorts(1)[0];
    }

    public static int[] getUsedPorts(int howMany) {
        final List<Integer> ports = new ArrayList<>(65535);
        int numFound = 0;
        for (int port = 1; port < 65535; port++) {
            if (numFound >= howMany) {
                break;
            }
            //noinspection EmptyTryBlock
            try (ServerSocket socket = new ServerSocket(port)) {
            } catch (IOException e) {
                ports.add(port);
                numFound++;
            }
        }
        return ports.stream().mapToInt(i -> i).toArray();
    }

    private SocketUtils() {}

}
