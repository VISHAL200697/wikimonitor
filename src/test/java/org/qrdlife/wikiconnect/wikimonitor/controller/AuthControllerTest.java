package org.qrdlife.wikiconnect.wikimonitor.controller;

import com.github.scribejava.core.model.OAuth2AccessToken;
import org.junit.jupiter.api.Test;
import org.qrdlife.wikiconnect.wikimonitor.config.SecurityConfig;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.service.OAuth2Service;
import org.qrdlife.wikiconnect.wikimonitor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private OAuth2Service oauth2Service;

    @Test
    public void testLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    public void testLoginWikimedia() throws Exception {
        when(oauth2Service.getAuthorizationUrl()).thenReturn("https://meta.wikimedia.org/w/rest.php/oauth2/authorize");

        mockMvc.perform(get("/auth/wikimedia"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://meta.wikimedia.org/w/rest.php/oauth2/authorize"));
    }

    @Test
    public void testOauthCallback() throws Exception {
        OAuth2AccessToken mockToken = new OAuth2AccessToken("access-token", "raw-response");
        User mockUser = new User(123L, "testuser");
        mockUser.setId(1L);
        mockUser.setRole("USER");
        mockUser.setApproved(true);

        when(oauth2Service.getAccessToken(anyString())).thenReturn(mockToken);
        when(oauth2Service.getUserInfo(any())).thenReturn(mockUser);
        when(userService.findOrCreateUser(anyLong(), anyString())).thenReturn(mockUser);

        mockMvc.perform(get("/oauth2/callback")
                        .param("code", "auth-code"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(oauth2Service).getAccessToken("auth-code");
        verify(userService).findOrCreateUser(123L, "testuser");
    }

    @Test
    public void testOauthCallbackError() throws Exception {
        when(oauth2Service.getAccessToken(anyString())).thenThrow(new RuntimeException("OAuth failed"));

        mockMvc.perform(get("/oauth2/callback")
                        .param("code", "auth-code"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}
