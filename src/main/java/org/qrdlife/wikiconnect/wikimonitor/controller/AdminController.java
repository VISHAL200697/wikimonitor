package org.qrdlife.wikiconnect.wikimonitor.controller;

import lombok.RequiredArgsConstructor;
import org.qrdlife.wikiconnect.wikimonitor.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final org.qrdlife.wikiconnect.wikimonitor.service.SettingsService settingsService;

    @GetMapping
    public String adminPage(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "") String search) {
        int pageSize = 20;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page,
                pageSize);

        org.springframework.data.domain.Page<org.qrdlife.wikiconnect.wikimonitor.model.User> userPage;
        if (search.isEmpty()) {
            userPage = userService.getAllUsers(pageable);
        } else {
            userPage = userService.searchUsers(search, pageable);
        }

        model.addAttribute("users", userPage);
        model.addAttribute("search", search);
        model.addAttribute("autoApproveEnabled", settingsService.isAutoApproveEnabled());
        return "admin";
    }

    @PostMapping("/users/{id}/approve")
    public String toggleApproval(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean approved) {
        userService.updateUserStatus(id, approved, userService.findAll().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(org.qrdlife.wikiconnect.wikimonitor.model.User::getRole)
                .orElse("USER"));
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam String role) {
        userService.updateUserStatus(id, userService.findAll().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(org.qrdlife.wikiconnect.wikimonitor.model.User::isApproved)
                .orElse(false), role);
        return "redirect:/admin";
    }

    @PostMapping("/settings/auto-approve")
    public String toggleAutoApprove(@RequestParam(defaultValue = "false") boolean enabled) {
        settingsService.setAutoApprove(enabled);
        return "redirect:/admin";
    }
}
