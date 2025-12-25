package com.chartdb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle SPA (Single Page Application) routing.
 * Forwards all non-API routes to index.html for client-side routing.
 */
@Controller
public class SpaController {

    /**
     * Forward all non-API, non-static routes to index.html
     * This enables client-side routing in the React app
     */
    @GetMapping(value = {
        "/",
        "/login",
        "/register",
        "/diagrams",
        "/diagrams/**",
        "/editor",
        "/editor/**",
        "/settings",
        "/settings/**",
        "/profile",
        "/profile/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
