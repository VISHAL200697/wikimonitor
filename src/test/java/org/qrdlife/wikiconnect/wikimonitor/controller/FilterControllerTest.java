package org.qrdlife.wikiconnect.wikimonitor.controller;

import org.junit.jupiter.api.Test;
import org.qrdlife.wikiconnect.wikimonitor.config.SecurityConfig;
import org.qrdlife.wikiconnect.wikimonitor.model.Filter;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.repository.UserRepository;
import org.qrdlife.wikiconnect.wikimonitor.service.FilterService;
import org.qrdlife.wikiconnect.wikimonitor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private FilterService filterService;

    @Test
    public void testGetFilters() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        
        Filter f = new Filter();
        f.setId(10L);
        f.setName("My Filter");
        f.setActive(true);

        when(userService.loadUserByUsername("testuser")).thenReturn(user);
        when(filterService.getUserFilters(user)).thenReturn(List.of(f));

        mockMvc.perform(get("/api/filters")
                .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("My Filter"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    public void testCreateFilter() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(user);

        Filter f = new Filter();
        f.setId(12L);
        when(filterService.createFilter(eq(user), eq("New Filter"), anyString())).thenReturn(f);

        String json = "{\"name\":\"New Filter\", \"filterCode\":\"type == 'edit'\"}";

        mockMvc.perform(post("/api/filters/create")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));

        verify(filterService).createFilter(eq(user), eq("New Filter"), anyString());
    }
    
    @Test
    public void testToggleFilter() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(user);

        String json = "{\"active\":true}";

        mockMvc.perform(post("/api/filters/1/toggle")
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf())
                .with(user("testuser").roles("USER")))
                .andExpect(status().isOk());

        verify(filterService).toggleFilterStatus(user, 1L, true);
    }
}
