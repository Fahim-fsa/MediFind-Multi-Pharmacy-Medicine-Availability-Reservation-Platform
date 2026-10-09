package com.medifind.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * SLP: Pharmacy Inventory & Listing Mgmt → "Upload a photo of the
 * medicine when adding it to the inventory"
 *
 * Uploaded medicine photos are saved by {@link com.medifind.service.FileStorageService}
 * to a plain directory on disk — deliberately outside {@code
 * src/main/resources/static}, since that folder is packaged into the
 * jar at build time and isn't writable once the app is running. This
 * maps requests for {@code /uploads/**} to that same directory, so a
 * saved file is servable exactly like any other static asset without
 * needing a dedicated controller/endpoint to stream its bytes back.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String uploadsDir;

    public WebMvcConfig(@Value("${medifind.uploads.dir:uploads}") String uploadsDir) {
        this.uploadsDir = uploadsDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadsDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}