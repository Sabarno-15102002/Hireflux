package com.sabarno.hireflux.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadDTO {

    @NotBlank
    private String fileKey;

    @NotBlank
    private String fileName;
}
