package edu.illinois.library.cantaloupe.delegate;

/**
 * Base class that {@link JavaDelegate}s can extend to get some free
 * functionality.
 */
@SuppressWarnings("unused")
public abstract class AbstractJavaDelegate {

    private JavaContext context;

    public final JavaContext getContext() {
        return context;
    }

    public final void setContext(JavaContext context) {
        this.context = context;
    }

}
