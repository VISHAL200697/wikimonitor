package org.qrdlife.wikiconnect.wikimonitor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(WikiController.class)
@Import(org.qrdlife.wikiconnect.wikimonitor.config.SecurityConfig.class)
public class WikiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testWikiPage() throws Exception {
        mockMvc.perform(get("/wiki")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("wiki"));
    }
}
