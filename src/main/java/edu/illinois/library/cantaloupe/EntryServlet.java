package edu.illinois.library.cantaloupe;

import org.restlet.data.Protocol;
import org.restlet.ext.servlet.ServerServlet;

import javax.servlet.ServletException;

/**
 * <p>Serves as the entry Servlet in both standalone and Servlet container
 * context.</p>
 *
 * <p>Because this is a Restlet application, there are no other Servlet
 * classes. Instead there are Restlet resource classes residing in
 * {@link edu.illinois.library.cantaloupe.resource}.</p>
 */
public class EntryServlet extends ServerServlet {

    private static final long serialVersionUID = 5627021158653885870L;

    @Override
    public void init() throws ServletException {
        super.init();

        getComponent().getClients().add(Protocol.CLAP);
    }

}
