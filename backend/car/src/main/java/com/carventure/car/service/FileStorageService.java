package com.carventure.car.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    // Define where files will reside on the server disk
    private final Path storageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public FileStorageService() {
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory.", e);
        }
    }

    public String saveFile(MultipartFile file) throws IOException {
        // Clean path to prevent directory traversal attacks
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        // Generate a unique filename to prevent overwriting existing files
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Resolve target path and copy stream
        Path targetLocation = this.storageLocation.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }
}

