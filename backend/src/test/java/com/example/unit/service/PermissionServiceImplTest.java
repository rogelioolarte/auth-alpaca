package com.example.unit.service;

import com.example.entity.Permission;
import com.example.persistence.impl.PermissionDAOImpl;
import com.example.resources.DataProvider;
import com.example.service.impl.PermissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock
    private PermissionDAOImpl dao;

    @InjectMocks
    private PermissionServiceImpl service;

    @Test
    void findById() {
        UUID id = UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133");

        when(this.dao.findById(id))
                .thenReturn(Optional.ofNullable(DataProvider.permissionMock()));
        Permission permission = this.service.findById(id);

        assertNotNull(permission);
        assertEquals(id, permission.getId());
        assertEquals("CREATE", permission.getPermissionName());
        verify(this.dao).findById(id);
    }

    @Test
    void findAllByIds() {
    }

    @Test
    void findAllByIdstoSet() {
    }

    @Test
    void save() {
    }

    @Test
    void saveAll() {
    }

    @Test
    void updateById() {
    }

    @Test
    void deleteById() {
    }

    @Test
    void findAll() {
    }

    @Test
    void findAllPage() {
    }

    @Test
    void existsById() {
    }

    @Test
    void existsAllByIds() {
    }

    @Test
    void existsByUniqueProperties() {
    }

    @Test
    void getDAO() {
    }

    @Test
    void getEntityName() {
    }
}