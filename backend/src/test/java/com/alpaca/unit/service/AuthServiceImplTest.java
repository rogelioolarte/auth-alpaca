package com.alpaca.unit.service;

import com.alpaca.security.manager.JJwtManager;
import com.alpaca.security.manager.PasswordManager;
import com.alpaca.service.impl.AuthServiceImpl;
import com.alpaca.service.impl.ProfileServiceImpl;
import com.alpaca.service.impl.RoleServiceImpl;
import com.alpaca.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private RoleServiceImpl roleService;
    @Mock
    private UserServiceImpl userService;
    @Mock
    private ProfileServiceImpl profileService;
    @Mock
    private JJwtManager jJwtManager;
    @Mock
    private PasswordManager passwordManager;

    @InjectMocks
    private AuthServiceImpl service;

    @Test
    void loadUser() {
    }

    @Test
    void setAttributesConverter() {
    }

    @Test
    void setRequestEntityConverter() {
    }

    @Test
    void setRestOperations() {
    }

    @Test
    void setSecurityContextBefore() {
    }

    @Test
    void login() {
    }

    @Test
    void register() {
    }

    @Test
    void loadUserByUsername() {
    }

    @Test
    void validateUserDetails() {
    }

    @Test
    void authenticate() {
    }

    @Test
    void testLoadUser() {
    }
}