package com.example.repository;

import com.example.entity.Role;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoleRepo extends GenericRepo<Role, UUID> {
}
