package edu.illinois.library.cantaloupe.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
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

    private SocketUtils() {}

}
