package com.example.resources;

import com.example.dto.request.PermissionRequestDTO;
import com.example.dto.response.PermissionResponseDTO;
import com.example.entity.Permission;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class PermissionProvider{

    public static PermissionRequestDTO firstPReqDTO = new PermissionRequestDTO("CREATE");
    public static PermissionRequestDTO secondPReqDTO = new PermissionRequestDTO("READ");

    public static PermissionResponseDTO firstPResDTO = new PermissionResponseDTO(
            UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2"), "DELETE");
    public static PermissionResponseDTO secondPResDTO = new PermissionResponseDTO(
            UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133"), "CREATE");

    public static List<Permission> listEntities() {
        return List.of(singleEntity(), alternativeEntity());
    }
    
    public static List<PermissionRequestDTO> listRequest() {
        return List.of(firstPReqDTO, secondPReqDTO);
    }
    
    public static List<PermissionResponseDTO> listResponse() {
        return List.of(firstPResDTO, secondPResDTO);
    }

    public static Permission singleEntity() {
        return new Permission(
                UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2"),
                "DELETE", new HashSet<>());
    }

    public static Permission alternativeEntity() {
        return new Permission(
                UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133"),
                "CREATE", new HashSet<>());
    }

    public static PermissionRequestDTO singleRequest() {
        return firstPReqDTO;
    }

    public static PermissionRequestDTO alternativeRequest() {
        return secondPReqDTO;
    }

    public static PermissionResponseDTO singleResponse() {
        return firstPResDTO;
    }

    public static PermissionResponseDTO alternativeResponse() {
        return secondPResDTO;
    }
}
