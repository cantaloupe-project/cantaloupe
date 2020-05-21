package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.Application;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class SystemUtils {

    private static final AtomicBoolean exitRequested     = new AtomicBoolean();
    private static final AtomicInteger requestedExitCode = new AtomicInteger();

    /**
     * Clears any exit request created by {@link #exit(int)}.
     */
    public static synchronized void clearExitRequest() {
        exitRequested.set(false);
        requestedExitCode.set(0);
    }

    /**
     * <p>Conditionally exits depending on the return value of {@link
     * Application#isTesting()}:</p>
     *
     * <ol>
     *     <li>If that method returns {@code false}, {@link System#exit(int)}
     *     is called.</li>
     *     <li>Otherwise, it is not called, but subsequent calls to {@link
     *     #exitRequested()} will return {@code true}, and {@link
     *     #requestedExitCode()} will return the requested exit code.</li>
     * </ol>
     *
     * @param code Status code.
     */
    public static void exit(int code) {
        if (Application.isTesting()) {
            exitRequested.set(true);
            requestedExitCode.set(code);
        } else {
            System.exit(code);
        }
    }

    /**
     * @return Whether {@link #exit(int)} has been invoked.
     */
    public static boolean exitRequested() {
        return exitRequested.get();
    }

    /**
     * @return Exit code passed to {@link #exit(int)}. This is meaningless
     *         unless {@link #exitRequested()} returns {@code true}.
     */
    public static int requestedExitCode() {
        return requestedExitCode.get();
    }

    private SystemUtils() {}

}
