package com.medifind.controller;

import com.medifind.dto.*;
import com.medifind.entity.User;
import com.medifind.enums.Role;
import com.medifind.exception.BusinessRuleException;
import com.medifind.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpServletRequest request, HttpServletResponse response,
                        RedirectAttributes redirectAttributes, Model model) {
        try {
            User user = authService.login(email, password, request, response);
            return "redirect:" + (user.getRole() == Role.PHARMACIST ? "/pharmacist/dashboard" : "/patient/dashboard");
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            return "auth/login";
        }
    }


    @GetMapping("/admin-login")
    public String adminLoginPage() {
        return "auth/admin-login";
    }

    @PostMapping("/admin-login")
    public String adminLoginStep1(@RequestParam String email, @RequestParam String password,
                                  HttpSession session, Model model) {
        try {
            authService.adminLoginStep1(email, password, session);
            return "redirect:/auth/admin-mfa";
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            return "auth/admin-login";
        }
    }

    @GetMapping("/admin-mfa")
    public String adminMfaPage(HttpSession session) {
        if (session.getAttribute("MEDIFIND_MFA_PENDING_USER_ID") == null) {
            return "redirect:/auth/admin-login";
        }
        return "auth/admin-mfa";
    }

    @PostMapping("/admin-mfa")
    public String adminMfaSubmit(@RequestParam String code, HttpSession session,
                                 HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            authService.completeAdminMfa(code, session, request, response);
            return "redirect:/admin/dashboard";
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/admin-mfa";
        }
    }



    @GetMapping("/register")
    public String registerChoice() {
        return "auth/register-choice";
    }

    @GetMapping("/register/patient")
    public String registerPatientPage(Model model) {
        if (!model.containsAttribute("registerPatientRequest")) {
            model.addAttribute("registerPatientRequest", new RegisterPatientRequest());
        }
        return "auth/register-patient";
    }

    @PostMapping("/register/patient")
    public String registerPatient(@Valid @ModelAttribute("registerPatientRequest") RegisterPatientRequest requestDto,
                                  BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register-patient";
        }
        try {
            authService.registerPatient(requestDto);
            redirectAttributes.addFlashAttribute("success", "Account created! Please sign in.");
            return "redirect:/auth/login";
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            requestDto.setPassword(null);
            requestDto.setConfirmPassword(null);
            return "auth/register-patient";
        }
    }

    @GetMapping("/register/pharmacist")
    public String registerPharmacistPage(Model model) {
        if (!model.containsAttribute("registerPharmacistRequest")) {
            model.addAttribute("registerPharmacistRequest", new RegisterPharmacistRequest());
        }
        return "auth/register-pharmacist";
    }

    @PostMapping("/register/pharmacist")
    public String registerPharmacist(@Valid @ModelAttribute("registerPharmacistRequest") RegisterPharmacistRequest requestDto,
                                     BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register-pharmacist";
        }
        try {
            authService.registerPharmacist(requestDto);
            String message = requestDto.isJoinExisting()
                    ? "Account created! Please sign in."
                    : "Account and pharmacy created! An admin will review your pharmacy before it appears in search — you can sign in now.";
            redirectAttributes.addFlashAttribute("success", message);
            return "redirect:/auth/login";
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            requestDto.setPassword(null);
            requestDto.setConfirmPassword(null);
            return "auth/register-pharmacist";
        }
    }


    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        if (!model.containsAttribute("forgotPasswordRequest")) {
            model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest requestDto,
                                 BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }
        authService.forgotPassword(requestDto.getEmail());
        model.addAttribute("success", "If that email has a MediFind account, we've sent a reset link to it.");
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        ResetPasswordRequest requestDto = new ResetPasswordRequest();
        requestDto.setToken(token);
        model.addAttribute("resetPasswordRequest", requestDto);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @ModelAttribute("resetPasswordRequest") ResetPasswordRequest requestDto,
                                BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }
        try {
            authService.resetPassword(requestDto);
            redirectAttributes.addFlashAttribute("success", "Password updated. Please sign in.");
            return "redirect:/auth/login";
        } catch (BusinessRuleException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/reset-password";
        }
    }
}
