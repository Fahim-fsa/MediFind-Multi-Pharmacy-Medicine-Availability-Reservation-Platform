package com.medifind.exception;

/**
 * SLP: Core Platform & Shared Engine → "Set up 3-tier project skeleton"
 *
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
