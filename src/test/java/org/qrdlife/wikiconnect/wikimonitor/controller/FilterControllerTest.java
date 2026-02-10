package org.qrdlife.wikiconnect.wikimonitor.controller;

import org.junit.jupiter.api.Test;
import org.qrdlife.wikiconnect.wikimonitor.config.SecurityConfig;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.repository.UserRepository;
import org.qrdlife.wikiconnect.wikimonitor.service.AbuseFilterService;
import org.qrdlife.wikiconnect.wikimonitor.service.UserService;
import org.qrdlife.wikiconnect.wikimonitor.service.WikiStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilterController.class)
@Import(SecurityConfig.class)
public class FilterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private AbuseFilterService abuseFilterService;

    @MockBean
    private WikiStreamService wikiStreamService;

    @Test
    @WithMockUser(username = "testuser")
    public void testGetFilterCode() throws Exception {
        User user = new User();
        user.setFilterCode("return true;");
        when(userService.loadUserByUsername("testuser")).thenReturn(user);

        mockMvc.perform(get("/api/filter/code"))
                .andExpect(status().isOk())
                .andExpect(content().string("return true;"));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testSaveFilterCode() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(user);

        mockMvc.perform(post("/api/filter/code")
                        .content("return false;")
                        .contentType("text/plain")) // Assuming text/plain as per controller logic implicitly, but check if
                // controller consumes JSON or plain text. Controller uses @RequestBody
                // String, so default might be JSON or plain text depending on negotiation
                // or config. Usually for String it's safer to not set content type or set
                // text/plain if configured.
                .andExpect(status().isOk());

        verify(userRepository).save(user);
        verify(abuseFilterService).refreshRules(user);
        verify(wikiStreamService).updateUser(user);
    }
}
