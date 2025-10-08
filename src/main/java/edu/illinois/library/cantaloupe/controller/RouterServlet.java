package edu.illinois.library.cantaloupe.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;

public class RouterServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)  throws IOException, ServletException {
        handle(request, response, "GET");
    }

    @Override
    protected void doHead(HttpServletRequest request,
                          HttpServletResponse response)  throws IOException, ServletException {
        handle(request, response, "HEAD");
    }

    @Override
    protected void doOptions(HttpServletRequest request,
                             HttpServletResponse response) throws IOException, ServletException {
        handle(request, response, "OPTIONS");
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException, ServletException {
        handle(request, response, "POST");
    }

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response)  throws IOException, ServletException {
        handle(request, response, "PUT");
    }

    private void handle(HttpServletRequest request,
                        HttpServletResponse response, String method) throws IOException, ServletException {
        String path = request.getServletPath();
        if (path.equals("/iiif/1/") && method.equals("GET")) {
            Controller controller = new edu.illinois.library.cantaloupe.controller.iiif.v1.LandingController();
            controller.doGet(request, response);(
        } else if (path.matches("^/iiif/1/[^/]*/info.json$") && method.equals("GET")) {
            Controller controller = new edu.illinois.library.cantaloupe.controller.iiif.v1.InformationController();
            controller.doGet(request, response);
        } else if (path.equals("/iiif/2/") && method.equals("GET")) {
            Controller controller = new edu.illinois.library.cantaloupe.controller.iiif.v2.LandingController();
            controller.doGet(request, response);
        } else if (path.equals("/iiif/3/") && method.equals("GET")) {        
            Controller controller = new edu.illinois.library.cantaloupe.controller.iiif.v2.LandingController();
            controller.doGet(request, response);
        } else {
            notFound(response);
        }
    }
    
    private void notFound(HttpServletResponse response) throws IOException {
        // Send a 404 response
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
