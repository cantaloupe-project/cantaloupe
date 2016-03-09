package edu.illinois.library.cantaloupe;

import org.restlet.ext.servlet.ServerServlet;

import javax.servlet.ServletException;

/**
 * Serves as the entry Servlet when the application is run in a Servlet
 * container. Referred to in web.xml.
 */
public class EntryServlet extends ServerServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        Application.initializeGeneral();
    }

    @Override
    public void destroy() {
        super.destroy();
        Application.shutdown();
    }

}
