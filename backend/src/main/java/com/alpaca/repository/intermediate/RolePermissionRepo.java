package com.alpaca.repository.intermediate;

import com.alpaca.entity.intermediate.RolePermission;
import com.alpaca.repository.GenericRepo;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepo extends GenericRepo<RolePermission, UUID> {}
