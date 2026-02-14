package org.qrdlife.wikiconnect.wikimonitor.controller;

import org.junit.jupiter.api.Test;
import org.qrdlife.wikiconnect.wikimonitor.config.SecurityConfig;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.repository.UserRepository;
import org.qrdlife.wikiconnect.wikimonitor.service.AbuseFilterService;
import org.qrdlife.wikiconnect.wikimonitor.service.UserService;
import org.qrdlife.wikiconnect.wikimonitor.service.WikiStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilterController.class)
@Import(SecurityConfig.class)
public class FilterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AbuseFilterService abuseFilterService;

    @MockitoBean
    private WikiStreamService wikiStreamService;

    @Test
    public void testGetFilterCode() throws Exception {
        User user = new User();
        user.setFilterCode("return true;");
        when(userService.loadUserByUsername("testuser")).thenReturn(user);

        mockMvc.perform(get("/api/filter/code")
                .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string("return true;"));
    }

    @Test
    public void testSaveFilterCode() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(user);

        mockMvc.perform(post("/api/filter/code")
                .content("return false;")
                .contentType("text/plain")
                .with(csrf())
                .with(user("testuser").roles("USER")))
                .andExpect(status().isOk());

        verify(userRepository).save(user);
        verify(abuseFilterService).refreshRules(user);
        verify(wikiStreamService).updateUser(user);
    }
}
