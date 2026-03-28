package org.qrdlife.wikiconnect.wikimonitor.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;
import org.qrdlife.wikiconnect.wikimonitor.service.OAuth2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import org.qrdlife.wikiconnect.wikimonitor.config.AuthConstants;

@Component
@Slf4j
public class TokenRenewalFilter extends OncePerRequestFilter {

    private final OAuth2Service oauth2Service;

    public TokenRenewalFilter(@Autowired(required = false) OAuth2Service oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (oauth2Service != null && session != null && session.getAttribute(AuthConstants.SESSION_ACCESS_TOKEN) != null) {
            Object mutex = org.springframework.web.util.WebUtils.getSessionMutex(session);
            synchronized (mutex) {
                Long expiryTime = (Long) session.getAttribute(AuthConstants.SESSION_ACCESS_TOKEN_EXPIRY);
                
                // Check if token expires within the threshold
                if (expiryTime != null && System.currentTimeMillis() > (expiryTime - AuthConstants.RENEWAL_THRESHOLD_MS)) {
                    String refreshToken = getRefreshTokenCookie(request);
                    if (refreshToken != null && !refreshToken.isEmpty()) {
                        try {
                            log.info("Access token is expiring soon, attempting silent renewal...");
                            var newToken = oauth2Service.refreshAccessToken(refreshToken);
                            session.setAttribute(AuthConstants.SESSION_ACCESS_TOKEN, newToken.getAccessToken());
                            
                            // Update expiration time
                            if (newToken.getExpiresIn() != null) {
                                session.setAttribute(AuthConstants.SESSION_ACCESS_TOKEN_EXPIRY, System.currentTimeMillis() + (newToken.getExpiresIn() * 1000L));
                            }
                            
                            // Possibly issue an updated refresh token cookie with SameSite support
                            if (newToken.getRefreshToken() != null && !newToken.getRefreshToken().isEmpty()) {
                                ResponseCookie refreshCookie = ResponseCookie.from(AuthConstants.COOKIE_REFRESH_TOKEN, newToken.getRefreshToken())
                                        .httpOnly(true)
                                        .secure(request.isSecure())
                                        .path("/")
                                        .maxAge(AuthConstants.COOKIE_MAX_AGE_SECONDS)
                                        .sameSite("Lax")
                                        .build();
                                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
                            }
                            log.info("Access token successfully renewed.");
                        } catch (Exception e) {
                            log.warn("Failed to silently renew access token: {}", e.getMessage());
                            // Proceed without interrupting. The API request will simply fail naturally, 
                            // or they'll be asked to re-login eventually.
                        }
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getRefreshTokenCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (AuthConstants.COOKIE_REFRESH_TOKEN.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
