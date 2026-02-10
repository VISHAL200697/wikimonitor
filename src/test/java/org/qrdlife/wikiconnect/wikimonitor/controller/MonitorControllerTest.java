package org.qrdlife.wikiconnect.wikimonitor.controller;

import org.junit.jupiter.api.Test;
import org.qrdlife.wikiconnect.wikimonitor.config.SecurityConfig;
import org.qrdlife.wikiconnect.wikimonitor.service.WikiStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitorController.class)
@Import(SecurityConfig.class)
public class MonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WikiStreamService streamService;

    @Test
    @WithMockUser
    public void testStream() throws Exception {
        when(streamService.subscribe(any())).thenReturn(new SseEmitter());

        mockMvc.perform(get("/stream"))
                .andExpect(status().isOk());

        verify(streamService).subscribe(any());
    }

    @Test
    @WithMockUser
    public void testPause() throws Exception {
        mockMvc.perform(post("/api/pause"))
                .andExpect(status().isOk());

        verify(streamService).setPaused(any(), eq(true));
    }

    @Test
    @WithMockUser
    public void testResume() throws Exception {
        mockMvc.perform(post("/api/resume"))
                .andExpect(status().isOk());

        verify(streamService).setPaused(any(), eq(false));
    }

    @Test
    @WithMockUser
    public void testStatus() throws Exception {
        when(streamService.isPaused(any())).thenReturn(true);

        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk());

        verify(streamService).isPaused(any());
    }
}
