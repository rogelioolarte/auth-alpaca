package com.alpaca.repository.intermediate;

import com.alpaca.entity.intermediate.RolePermission;
import com.alpaca.repository.GenericRepo;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RolePermissionRepo extends GenericRepo<RolePermission, UUID> {
}
