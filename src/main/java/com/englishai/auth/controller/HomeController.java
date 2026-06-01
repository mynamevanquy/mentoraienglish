package com.englishai.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller serving public static pages: landing homepage and pricing options.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "public/home";
    }

    @GetMapping("/pricing")
    public String pricing() {
        return "public/pricing";
    }
}
