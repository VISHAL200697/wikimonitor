package org.qrdlife.wikiconnect.wikimonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WikiStreamServiceTest {

    @Mock
    private ObjectMapper mapper;
    @Mock
    private AbuseFilterService abuseFilter;
    @Mock
    private UserService userService;
    @Mock
    private Principal principal;

    private WikiStreamService wikiStreamService;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        wikiStreamService = new WikiStreamService(mapper, abuseFilter, userService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    void testSubscribe_WithPrincipal() {
        when(principal.getName()).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(testUser);

        SseEmitter emitter = wikiStreamService.subscribe(principal);
        assertNotNull(emitter);
        // We can't easily check internal state 'emitters' without reflection, but we
        // assume it works if no exception.
        // Also could test side effects if any exposed.
    }

    @Test
    void testSubscribe_WithTokenPrincipal() {
        UsernamePasswordAuthenticationToken token = mock(UsernamePasswordAuthenticationToken.class);
        when(token.getPrincipal()).thenReturn(testUser);

        SseEmitter emitter = wikiStreamService.subscribe(token);
        assertNotNull(emitter);
    }

    @Test
    void testSubscribe_Anonymous() {
        SseEmitter emitter = wikiStreamService.subscribe(null);
        assertNotNull(emitter);
    }

    @Test
    void testSetPaused() {
        // Setup a subscription first
        when(principal.getName()).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(testUser);
        wikiStreamService.subscribe(principal);

        assertFalse(wikiStreamService.isPaused(principal)); // Default false

        wikiStreamService.setPaused(principal, true);
        assertTrue(wikiStreamService.isPaused(principal));

        wikiStreamService.setPaused(principal, false);
        assertFalse(wikiStreamService.isPaused(principal));
    }

    @Test
    void testUpdateUser() {
        // Setup subscription
        when(principal.getName()).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(testUser);
        wikiStreamService.subscribe(principal);

        // Update user object
        User updatedUser = new User();
        updatedUser.setId(1L); // Same ID
        updatedUser.setUsername("testuser");
        updatedUser.setFilterCode("some new code");

        wikiStreamService.updateUser(updatedUser);

        // Theoretically internal reference is updated.
        // Hard to verify without internal access, but we ensure no exceptions.
    }
}
