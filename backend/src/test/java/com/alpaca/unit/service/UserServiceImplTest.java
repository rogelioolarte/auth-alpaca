package com.alpaca.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alpaca.entity.User;
import com.alpaca.exception.BadRequestException;
import com.alpaca.persistence.IUserDAO;
import com.alpaca.resources.UserProvider;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.impl.UserServiceImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Unit tests for {@link UserServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private IUserDAO dao;
    @Mock private PasswordManager passwordManager;

    @InjectMocks private UserServiceImpl service;

    private User firstUser;
    private User secondUser;
    private final String encodedPw = "encoded_password";

    @BeforeEach
    void setup() {
        firstUser = UserProvider.singleEntity();
        secondUser = UserProvider.alternativeEntity();
    }

    @Test
    void save_DelegatesToRegister() {
        when(passwordManager.encodePassword(firstUser.getPassword())).thenReturn(encodedPw);
        when(dao.save(firstUser)).thenReturn(firstUser);

        User result = service.save(firstUser);

        assertEquals(firstUser, result);
        verify(dao).save(firstUser);
    }

    @Test
    void updateById_WhenUserIsNull_ThrowsBadRequestException() {
        UUID id = secondUser.getId();
        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> service.updateById(null, id));
        assertTrue(exception.getMessage().contains("User"));
    }

    @Test
    void updateById_WhenUuidIsNull_ThrowsBadRequestException() {
        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> service.updateById(firstUser, null));
        assertTrue(exception.getMessage().contains("User"));
    }

    @Test
    void updateById_WhenValid_ReturnsUpdatedUser() {
        UUID id = firstUser.getId();
        when(dao.updateById(firstUser, id)).thenReturn(firstUser);

        User result = service.updateById(firstUser, id);

        assertEquals(firstUser, result);
        verify(dao).updateById(firstUser, id);
    }

    @Test
    void register_WhenUserIsNull_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> service.save(null));
    }

    @Test
    void register_WhenValid_EncodesPasswordAndSaves() {
        when(passwordManager.encodePassword(secondUser.getPassword())).thenReturn(encodedPw);
        when(dao.save(secondUser)).thenReturn(secondUser);

        User result = service.save(secondUser);

        assertEquals(secondUser, result);
        assertEquals(encodedPw, secondUser.getPassword());
        verify(dao).save(secondUser);
    }

    @Test
    void existsByEmail_ReturnsBoolean() {
        String email = firstUser.getEmail();
        when(dao.existsByEmail(email)).thenReturn(true);

        boolean exists = service.existsByEmail(email);

        assertTrue(exists);
        verify(dao).existsByEmail(email);
    }

    @Test
    void findByEmail_WhenEmailIsNull_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> service.findByEmail(null));
    }

    @Test
    void findByEmail_WhenEmailIsBlank_ThrowsBadRequestException() {
        String blankEmail = "   ";
        assertThrows(BadRequestException.class, () -> service.findByEmail(blankEmail));
    }

    @Test
    void findByEmail_WhenUserNotFound_ThrowsUsernameNotFoundException() {
        String email = secondUser.getEmail();
        when(dao.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.findByEmail(email));
    }

    @Test
    void findByEmail_WhenUserExists_ReturnsUser() {
        String email = firstUser.getEmail();
        when(dao.findByEmail(email)).thenReturn(Optional.of(firstUser));

        User result = service.findByEmail(email);

        assertEquals(firstUser, result);
        verify(dao).findByEmail(email);
    }
}
