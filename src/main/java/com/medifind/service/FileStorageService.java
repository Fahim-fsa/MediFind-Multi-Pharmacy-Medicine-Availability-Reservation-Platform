package com.medifind.service;

import com.medifind.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SLP: Pharmacy Inventory & Listing Mgmt → "Upload a photo of the
 * medicine when adding it to the inventory"
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** Keeps this in sync with WebMvcConfig's resource handler mapping. */
    private static final String PUBLIC_PATH_PREFIX = "/uploads/medicines/";

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");

    private final Path uploadRoot;

    public FileStorageService(@Value("${medifind.uploads.dir:uploads}") String uploadsDir) {
        this.uploadRoot = Paths.get(uploadsDir, "medicines").toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException ex) {
            log.error("Could not create upload directory {}", uploadRoot, ex);
        }
    }


    public String storeMedicineImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String extension = extensionFor(file);
        if (extension == null) {
            throw new BusinessRuleException(
                    "Please upload a medicine photo as a PNG, JPEG, or WEBP image.");
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path destination = uploadRoot.resolve(filename);
        try (var in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Failed to save uploaded medicine image", ex);
            throw new BusinessRuleException("Could not save that image right now. Please try again.");
        }
        return PUBLIC_PATH_PREFIX + filename;
    }


    private String extensionFor(MultipartFile file) {
        String byContentType = ALLOWED_CONTENT_TYPES.get(file.getContentType());
        if (byContentType != null) {
            return byContentType;
        }
        String original = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (original != null && ALLOWED_EXTENSIONS.contains(original.toLowerCase())) {
            return original.toLowerCase();
        }
        return null;
    }
}
