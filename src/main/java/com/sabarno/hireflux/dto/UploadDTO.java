package com.sabarno.hireflux.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadDTO {

    @NotBlank(message = "File key cannot be blank")
    private String fileKey;

    @NotBlank(message = "File name cannot be blank")
    private String fileName;
}
