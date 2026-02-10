package org.qrdlife.wikiconnect.wikimonitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@lombok.extern.slf4j.Slf4j
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        log.info("Serving index page");
        return "index";
    }
}
