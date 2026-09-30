package com.medifind.exception;

/**
 * SLP: Core Platform & Shared Engine → "Set up 3-tier project skeleton"
 *
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
