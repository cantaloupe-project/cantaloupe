package edu.illinois.library.cantaloupe.controller;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class Controller {
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

    }
}
