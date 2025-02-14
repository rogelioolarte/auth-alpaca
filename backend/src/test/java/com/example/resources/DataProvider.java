package com.example.resources;

import com.example.entity.Permission;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DataProvider {

    static Permission firstPermission = new Permission(
            UUID.fromString("c06f3206-c469-4216-bbc7-77fed3a8a133"),
            "CREATE", Set.of());
    static Permission secondPermission = new Permission(
            UUID.fromString("b1f383ce-4c1e-4d0e-bb43-a9674377c4a2"),
            "DELETE", Set.of());
    static Permission thirthPermission = new Permission(
            UUID.fromString("55d1558a-dabe-424f-b54f-a8168b950a5b"),
            "READ", Set.of());
    static Permission fourthPermission = new Permission(
            UUID.fromString("3860ff68-67ad-475c-a10d-470ebac9c092"),
            "README", Set.of());
    static Permission fifthPermission = new Permission(
            UUID.fromString("db506e4a-cbfa-4b73-a248-5cf7501513f7"),
            "UPDATE", Set.of());

    public static List<Permission> permissionsMock() {
        return List.of(firstPermission, secondPermission, thirthPermission,
                fourthPermission, fifthPermission);
    }

    public static Permission permissionMock() {
        return firstPermission;
    }
}
