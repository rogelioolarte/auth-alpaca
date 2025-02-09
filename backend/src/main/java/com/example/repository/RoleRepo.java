package com.example.repository;

import com.example.entity.Role;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepo extends GenericRepo<Role, UUID> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_roles WHERE role_id = ?1", nativeQuery = true)
    void deleteUserRolesByRoleId(UUID roleId);

    Optional<Role> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);

    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    List<Role> findRolesByPermissionId(@Param("permissionId") UUID permissionId);
}

