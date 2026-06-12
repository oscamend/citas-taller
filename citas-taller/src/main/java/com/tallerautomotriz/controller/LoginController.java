package com.tallerautomotriz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String verLogin() {
        return "login"; // Esto busca el archivo login.html en templates
    }
}