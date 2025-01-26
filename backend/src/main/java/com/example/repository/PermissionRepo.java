package com.example.repository;

import com.example.entity.Permission;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PermissionRepo extends GenericRepo<Permission, UUID> {
}
