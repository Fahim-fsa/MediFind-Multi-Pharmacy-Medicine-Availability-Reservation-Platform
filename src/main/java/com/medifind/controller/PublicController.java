package com.medifind.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SLP: Core Platform & Shared Engine → "Set up 3-tier project skeleton"
 * SLP: Medicine Search & Pharmacy Discovery (UI/UX Design → Landing page)
 *
 */
@Controller
public class PublicController {

    @GetMapping("/")
    public String landing() {
        return "index";
    }
}
