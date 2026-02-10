package org.qrdlife.wikiconnect.wikimonitor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.qrdlife.wikiconnect.wikimonitor.service.MediaWikiService;
import org.qrdlife.wikiconnect.wikimonitor.service.OAuth2Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for handling Wiki actions such as undo and rollback.
 * Uses MediaWiki API via OAuth2Service.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class WikiActionController {

    private final OAuth2Service oauth2Service;
    private final ObjectMapper objectMapper;

    /**
     * Performs an undo action on a specific revision.
     *
     * @param serverName The server domain (e.g., en.wikipedia.org).
     * @param title      The title of the page.
     * @param revision   The revision ID to undo.
     * @param summary    Optional summary for the undo action.
     * @param session    The HTTP session containing the access token.
     * @return A ResponseEntity containing the JSON response from the MediaWiki API.
     */
    @PostMapping("/api/action/undo")
    public ResponseEntity<?> undo(
            @RequestParam String serverName,
            @RequestParam String title,
            @RequestParam long revision,
            @RequestParam(required = false) String summary,
            HttpSession session) {

        log.info("Undo requested for title: {}, revision: {}, server: {}, summary: {}", title, revision, serverName,
                summary);

        String token = (String) session.getAttribute("ACCESS_TOKEN");
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        try {
            String apiUrl = "https://" + serverName + "/w/api.php";
            var api = oauth2Service.getActionApi(token, apiUrl);
            var mediaWikiService = new MediaWikiService(api);

            String editResponse = mediaWikiService.undoEdit(title, revision, summary);

            log.info("Undo successful for title: {}, response: {}", title, editResponse);
            JsonNode jsonResponse = objectMapper.readTree(editResponse);
            return ResponseEntity.ok(jsonResponse);

        } catch (Exception e) {
            log.error("Error performing undo for title: " + title, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Performs a rollback action for a user's edits on a page.
     *
     * @param serverName The server domain.
     * @param title      The title of the page.
     * @param user       The user whose edits are to be rolled back.
     * @param session    The HTTP session containing the access token.
     * @return A ResponseEntity containing the JSON response from the MediaWiki API.
     */
    @PostMapping("/api/action/rollback")
    public ResponseEntity<?> rollback(
            @RequestParam String serverName,
            @RequestParam(required = false) String title,
            @RequestParam String user,
            HttpSession session) {

        log.info("Rollback requested for title: {}, user: {}, server: {}", title, user, serverName);

        String token = (String) session.getAttribute("ACCESS_TOKEN");
        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        }

        try {
            String apiUrl = "https://" + serverName + "/w/api.php";
            var api = oauth2Service.getActionApi(token, apiUrl);
            var mediaWikiService = new MediaWikiService(api);

            String rbResponse = mediaWikiService.rollbackEdit(title, user);

            log.info("Rollback successful for title: {}, response: {}", title, rbResponse);
            JsonNode jsonResponse = objectMapper.readTree(rbResponse);
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            log.error("Error performing rollback for title: " + title, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
