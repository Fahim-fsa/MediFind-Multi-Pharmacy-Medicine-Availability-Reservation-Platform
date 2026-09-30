package com.medifind.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * SLP: Core Platform & Shared Engine → "Set up 3-tier project skeleton
 * (Spring Boot, MVC, REST)"
 *
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.info("Resource not found: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
        }
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public Object handleBusinessRule(BusinessRuleException ex, HttpServletRequest request, Model model) {
        log.info("Business rule violation: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
        }
        ModelAndView mav = new ModelAndView("error/403");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    /**
     * SLP: Pharmacy Inventory & Listing Mgmt → "Upload a photo of the
     * medicine when adding it to the inventory"
     *
     * Without this, a photo over spring.servlet.multipart.max-file-size
     * would otherwise fall through to {@link #handleUnexpected} and show
     * the generic "something broke on our end" page — misleading for
     * what's really just an oversized upload, not a server bug.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleUploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.info("Upload rejected as too large on {}", request.getRequestURI());
        String message = "That file is too large. Please upload an image under 10 MB.";
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest().body(Map.of("error", message));
        }
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", message);
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        if (isApiRequest(request)) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Something went wrong. Please try again."));
        }
        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }
}
