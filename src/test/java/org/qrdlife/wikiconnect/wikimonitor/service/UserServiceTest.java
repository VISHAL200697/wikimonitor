package org.qrdlife.wikiconnect.wikimonitor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.qrdlife.wikiconnect.wikimonitor.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@lombok.extern.slf4j.Slf4j
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.qrdlife.wikiconnect.wikimonitor.service.SettingsService settingsService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, settingsService);
    }

    @Test
    void loadUserByUsername_Success() {
        User user = new User(1L, "testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void loadUserByUsername_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("unknown"));
    }

    @Test
    void findAll_Success() {
        User user = new User(1L, "testuser");
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));

        List<User> result = userService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
    }

    @Test
    void updateUserStatus_Success() {
        User user = new User(1L, "testuser");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateUserStatus(1L, true, "ADMIN");

        verify(userRepository).save(user);
        assertTrue(user.isApproved());
        assertEquals("ADMIN", user.getRole());
    }

    @Test
    void updateUserStatus_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        userService.updateUserStatus(1L, true, "ADMIN");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findOrCreateUser_UpdateUsername() {
        Long centralId = 12345L;
        String oldUsername = "oldName";
        String newUsername = "newName";

        User existingUser = new User(centralId, oldUsername);
        existingUser.setId(1L);

        when(userRepository.findByCentralId(centralId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.findOrCreateUser(centralId, newUsername);

        assertEquals(newUsername, result.getUsername());
        verify(userRepository).save(existingUser);
        log.info("Verified username update: {} -> {}", oldUsername, result.getUsername());
    }

}
