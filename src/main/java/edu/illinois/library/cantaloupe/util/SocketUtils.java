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
