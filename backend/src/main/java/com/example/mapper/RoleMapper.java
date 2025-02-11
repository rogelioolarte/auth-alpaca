package com.example.mapper;

import com.example.dto.request.RoleRequestDTO;
import com.example.dto.response.RoleResponseDTO;
import com.example.entity.Role;
import com.example.qualifier.FindEntitiesSet;
import com.example.qualifier.MainService;
import com.example.service.Impl.PermissionServiceImpl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = { PermissionServiceImpl.class })
public interface RoleMapper
        extends GenericMapper<Role, RoleResponseDTO, RoleRequestDTO> {

    @Override
    @Mapping(source = "id", target = "id")
    @Mapping(source = "roleName", target = "roleName")
    @Mapping(source = "roleDescription", target = "roleDescription")
    @Mapping(source = "permissions", target = "permissions")
    RoleResponseDTO toResponseDTO(Role role);


    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "roleName", target = "roleName")
    @Mapping(source = "roleDescription", target = "roleDescription")
    @Mapping(source = "permissions", target = "permissions",
            qualifiedBy = { MainService.class, FindEntitiesSet.class })
    @Mapping(target = "users", ignore = true)
    Role toEntity(RoleRequestDTO dto);

}
