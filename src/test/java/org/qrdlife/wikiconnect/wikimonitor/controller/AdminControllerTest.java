package org.qrdlife.wikiconnect.wikimonitor.controller;

import org.junit.jupiter.api.Test;
import org.qrdlife.wikiconnect.wikimonitor.config.SecurityConfig;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyBoolean;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private org.qrdlife.wikiconnect.wikimonitor.service.SettingsService settingsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminPage() throws Exception {
        User user = new User(1L, "testuser");
        Page<User> page = new PageImpl<>(Collections.singletonList(user));

        when(userService.getAllUsers(any(PageRequest.class))).thenReturn(page);
        when(settingsService.isAutoApproveEnabled()).thenReturn(false);

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testAdminPageAccessDenied() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is4xxClientError()); // 403 Forbidden
        // .andExpect(forwardedUrl("/access-denied")); // Depending on config
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testToggleApproval() throws Exception {
        User user = new User(1L, "testuser");
        user.setId(1L);
        when(userService.findAll()).thenReturn(List.of(user));

        mockMvc.perform(post("/admin/users/1/approve")
                .param("approved", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(userService).updateUserStatus(eq(1L), eq(true), anyString());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateRole() throws Exception {
        User user = new User(1L, "testuser");
        user.setId(1L);
        when(userService.findAll()).thenReturn(List.of(user));

        mockMvc.perform(post("/admin/users/1/role")
                .param("role", "ADMIN")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(userService).updateUserStatus(eq(1L), anyBoolean(), eq("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAutoApproveToggle() throws Exception {
        mockMvc.perform(post("/admin/settings/auto-approve")
                .param("enabled", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(settingsService).setAutoApprove(true);
    }
}
