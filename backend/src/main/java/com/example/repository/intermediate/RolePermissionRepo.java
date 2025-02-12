package com.example.repository.intermediate;

import com.example.entity.intermediate.RolePermission;
import com.example.repository.GenericRepo;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RolePermissionRepo extends GenericRepo<RolePermission, UUID> {
}
