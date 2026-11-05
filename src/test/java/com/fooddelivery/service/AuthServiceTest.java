package com.fooddelivery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fooddelivery.domain.Role;
import com.fooddelivery.domain.User;
import com.fooddelivery.exception.AuthenticationException;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.util.PasswordHasher;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests US1.1 using a mocked {@link UserRepository}. */
class AuthServiceTest {

    private UserRepository repository;
    private AuthService service;
    private User alice;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        service = new AuthService(repository);
        alice = new User("u1", "alice", PasswordHasher.hash("pw123"), Role.CUSTOMER);
    }

    @Test
    void validCredentialsLogIn() {
        when(repository.findByUsername("alice")).thenReturn(Optional.of(alice));
        assertSame(alice, service.login("alice", "pw123"));
    }

    @Test
    void wrongPasswordIsRejected() {
        when(repository.findByUsername("alice")).thenReturn(Optional.of(alice));
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> service.login("alice", "wrong"));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void unknownUserIsRejectedWithSameMessage() {
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> service.login("ghost", "pw123"));
        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void nullOrBlankInputIsRejectedWithoutQueryingRepository() {
        assertThrows(AuthenticationException.class, () -> service.login(null, "pw"));
        assertThrows(AuthenticationException.class, () -> service.login("  ", "pw"));
        assertThrows(AuthenticationException.class, () -> service.login("alice", null));
        assertThrows(AuthenticationException.class, () -> service.login("alice", ""));
        verify(repository, never()).findByUsername(anyString());
    }
}
