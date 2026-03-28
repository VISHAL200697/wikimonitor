package org.qrdlife.wikiconnect.wikimonitor.filter;

import com.github.scribejava.core.model.OAuth2AccessToken;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qrdlife.wikiconnect.wikimonitor.service.OAuth2Service;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import jakarta.servlet.http.Cookie;

import org.qrdlife.wikiconnect.wikimonitor.config.AuthConstants;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenRenewalFilterTest {

    @Mock
    private OAuth2Service oauth2Service;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private TokenRenewalFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        session = new MockHttpSession();
    }

    @Test
    void doFilterInternal_shouldNotRenew_whenTokenNotExpiringSoon() throws Exception {
        session.setAttribute(AuthConstants.SESSION_ACCESS_TOKEN, "old-token");
        // Expiring in 1 hour
        session.setAttribute(AuthConstants.SESSION_ACCESS_TOKEN_EXPIRY, System.currentTimeMillis() + 3600_000L);
        request.setSession(session);
        request.setCookies(new Cookie(AuthConstants.COOKIE_REFRESH_TOKEN, "some-refresh-token"));

        filter.doFilter(request, response, filterChain);

        verify(oauth2Service, never()).refreshAccessToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldRenew_whenTokenIsExpiringSoon() throws Exception {
        session.setAttribute(AuthConstants.SESSION_ACCESS_TOKEN, "old-token");
        // Expiring in 1 minute (within the 5 min threshold)
        session.setAttribute(AuthConstants.SESSION_ACCESS_TOKEN_EXPIRY, System.currentTimeMillis() + 60_000L);
        request.setSession(session);
        request.setCookies(new Cookie(AuthConstants.COOKIE_REFRESH_TOKEN, "valid-refresh-token"));

        OAuth2AccessToken newToken = new OAuth2AccessToken("new-access-token", "bearer", 3600, "new-refresh-token", "scope", "raw");
        when(oauth2Service.refreshAccessToken("valid-refresh-token")).thenReturn(newToken);

        filter.doFilter(request, response, filterChain);

        verify(oauth2Service).refreshAccessToken("valid-refresh-token");
        verify(filterChain).doFilter(request, response);

        assertEquals("new-access-token", session.getAttribute(AuthConstants.SESSION_ACCESS_TOKEN));
        assertTrue((Long) session.getAttribute(AuthConstants.SESSION_ACCESS_TOKEN_EXPIRY) > System.currentTimeMillis() + 3500_000L);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains(AuthConstants.COOKIE_REFRESH_TOKEN + "=new-refresh-token"));
        assertTrue(setCookieHeader.contains("SameSite=Lax"));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("Path=/"));
    }

    @Test
    void doFilterInternal_shouldNotRenew_whenNoRefreshTokenCookie() throws Exception {
        session.setAttribute(AuthConstants.SESSION_ACCESS_TOKEN, "old-token");
        // Expiring deeply
        session.setAttribute(AuthConstants.SESSION_ACCESS_TOKEN_EXPIRY, System.currentTimeMillis() - 10_000L);
        request.setSession(session);
        // No cookies set

        filter.doFilter(request, response, filterChain);

        verify(oauth2Service, never()).refreshAccessToken(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
