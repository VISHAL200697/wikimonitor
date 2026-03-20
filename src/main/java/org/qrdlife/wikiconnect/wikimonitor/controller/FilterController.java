package org.qrdlife.wikiconnect.wikimonitor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.qrdlife.wikiconnect.wikimonitor.exception.MaxActiveFiltersExceededException;
import org.qrdlife.wikiconnect.wikimonitor.model.Filter;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.service.FilterService;
import org.qrdlife.wikiconnect.wikimonitor.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FilterController {

    private final UserService userService;
    private final FilterService filterService;

    @GetMapping("/api/filters")
    public ResponseEntity<List<Filter>> getUserFilters(Principal principal) {
        User user = (User) userService.loadUserByUsername(principal.getName());
        return ResponseEntity.ok(filterService.getUserFilters(user));
    }

    @Deprecated
    @GetMapping("/api/filter/code")
    public String getFilterCode(Principal principal) {
        return "";
    }

    @Deprecated
    @PostMapping("/api/filter/code")
    public void saveFilterCode(@RequestBody String code, Principal principal) {
        // Deprecated
    }

    @PostMapping("/api/filters/create")
    public ResponseEntity<?> createFilter(@RequestBody Map<String, String> payload, Principal principal) {
        String name = payload.get("name");
        String code = payload.get("filterCode");
        User user = (User) userService.loadUserByUsername(principal.getName());
        
        try {
            Filter filter = filterService.createFilter(user, name, code);
            return ResponseEntity.ok(filter);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (MaxActiveFiltersExceededException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/filters/{id}/toggle")
    public ResponseEntity<?> toggleFilter(@PathVariable Long id, @RequestBody Map<String, Boolean> payload, Principal principal) {
        User user = (User) userService.loadUserByUsername(principal.getName());
        boolean active = payload.getOrDefault("active", false);
        try {
            filterService.toggleFilterStatus(user, id, active);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (MaxActiveFiltersExceededException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/filters/{id}/delete")
    public ResponseEntity<?> deleteFilter(@PathVariable Long id, Principal principal) {
        User user = (User) userService.loadUserByUsername(principal.getName());
        try {
            filterService.deleteFilter(user, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/filters/{id}/rename")
    public ResponseEntity<?> renameFilter(@PathVariable Long id, @RequestBody Map<String, String> payload, Principal principal) {
        User user = (User) userService.loadUserByUsername(principal.getName());
        String name = payload.get("name");
        try {
            filterService.updateFilterName(user, id, name);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/api/filters/{id}/update")
    public ResponseEntity<?> updateFilter(@PathVariable Long id, @RequestBody Map<String, String> payload, Principal principal) {
        User user = (User) userService.loadUserByUsername(principal.getName());
        String name = payload.get("name");
        String code = payload.get("filterCode");
        try {
            filterService.updateFilter(user, id, name, code);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
