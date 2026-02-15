package org.qrdlife.wikiconnect.wikimonitor.controller;

import lombok.RequiredArgsConstructor;
import org.qrdlife.wikiconnect.wikimonitor.service.OAuth2Service;
import org.qrdlife.wikiconnect.wikimonitor.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AuthController {

    private final UserService userService;

    private final OAuth2Service oauth2Service;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/auth/wikimedia")
    public String loginWikimedia(jakarta.servlet.http.HttpServletRequest request) {
        log.info("Redirecting to Wikimedia OAuth");
        String state = java.util.UUID.randomUUID().toString();
        request.getSession().setAttribute("OAUTH_STATE", state);
        return "redirect:" + oauth2Service.getAuthorizationUrl(state);
    }

    @GetMapping("/oauth2/callback")
    public String oauthCallback(@RequestParam String code, @RequestParam String state,
            jakarta.servlet.http.HttpServletRequest request) {
        log.info("OAuth callback received with code length: {}", code.length());

        String sessionState = (String) request.getSession().getAttribute("OAUTH_STATE");
        if (sessionState == null || !sessionState.equals(state)) {
            log.error("OAuth state mismatch! Expected: {}, Received: {}", sessionState, state);
            return "redirect:/login?error=invalid_state";
        }
        request.getSession().removeAttribute("OAUTH_STATE");

        try {
            var token = oauth2Service.getAccessToken(code);
            var user = oauth2Service.getUserInfo(token);
            var userDetails = userService.findOrCreateUser(user.getCentralId(), user.getUsername());

            // Manually set security context
            var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT",
                    org.springframework.security.core.context.SecurityContextHolder.getContext());
            request.getSession().setAttribute("ACCESS_TOKEN", token.getAccessToken());

            log.info("User logged in successfully: {} (Local ID: {})", userDetails.getUsername(), userDetails.getId());
            return "redirect:/";
        } catch (Exception e) {
            log.error("OAuth login failed", e);
            return "redirect:/login?error";
        }
    }
}
