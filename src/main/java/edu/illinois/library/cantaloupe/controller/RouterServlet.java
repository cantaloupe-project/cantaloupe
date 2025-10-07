package edu.illinois.library.cantaloupe.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import java.io.IOException;

import org.apache.jena.sparql.function.library.e;

public class RouterServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)  throws IOException, ServletException {
        handle(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request,
                          HttpServletResponse response) throws IOException, ServletException {
        handle(request, response);
    }

    @Override
    protected void doOptions(HttpServletRequest request,
                             HttpServletResponse response) throws IOException, ServletException {
        handle(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)  throws IOException, ServletException {
        handle(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response)  throws IOException, ServletException {
        handle(request, response);
    }

    private void handle(HttpServletRequest request,
                        HttpServletResponse response) throws IOException, ServletException {
        String path = request.getServletPath();
        RequestDispatcher dispatcher = request.getRequestDispatcher(findServlet(path));
        dispatcher.forward(request, response);
    }

    private String findServlet(String path) {
        if (path.equals("/iiif/1/")) {
            return "iiifV1Landing";
        } else if (path.startsWith("/static/")) {
            return "staticFiles";
        } else {
            return "handler";
        }
    }
}
