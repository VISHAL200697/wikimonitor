package org.qrdlife.wikiconnect.wikimonitor.controller;

import lombok.RequiredArgsConstructor;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.repository.UserRepository;
import org.qrdlife.wikiconnect.wikimonitor.service.AbuseFilterService;
import org.qrdlife.wikiconnect.wikimonitor.service.UserService;
import org.qrdlife.wikiconnect.wikimonitor.service.WikiStreamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class FilterController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final AbuseFilterService abuseFilterService;
    private final WikiStreamService wikiStreamService;

    @GetMapping("/api/filter/code")
    public String getFilterCode(Principal principal) {
        User user = (User) userService.loadUserByUsername(principal.getName());
        return user.getFilterCode() != null ? user.getFilterCode() : "";
    }

    @PostMapping("/api/filter/code")
    public void saveFilterCode(@RequestBody String code, Principal principal) {
        log.info("Updating filter code for user: {}", principal.getName());
        User user = (User) userService.loadUserByUsername(principal.getName());
        user.setFilterCode(code);
        userRepository.save(user); // We need UserRepository to save changes to User
        abuseFilterService.refreshRules(user);
        wikiStreamService.updateUser(user);
        log.info("Filter code updated and refreshed for user: {}", user.getUsername());
    }
}
