package com.alpaca.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PermissionRequestDTO {

    @Size(min = 4, max = 25, message = "Permission Name must be at least 4 to 25 characters.")
    @NotBlank(message = "Permission Name is required.")
    String permissionName;
}
