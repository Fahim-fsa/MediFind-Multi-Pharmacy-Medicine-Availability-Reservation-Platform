package com.medifind.service;

import com.medifind.dto.RegisterPatientRequest;
import com.medifind.dto.RegisterPharmacistRequest;
import com.medifind.dto.ResetPasswordRequest;
import com.medifind.entity.Pharmacy;
import com.medifind.entity.User;
import com.medifind.enums.Role;
import com.medifind.enums.UserStatus;
import com.medifind.enums.VerificationStatus;
import com.medifind.exception.BusinessRuleException;
import com.medifind.repository.PharmacyRepository;
import com.medifind.repository.UserRepository;
import com.medifind.security.CustomUserDetails;
import com.medifind.util.CodeGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SLP: Patient Authentication & Profile / Pharmacy Onboarding &
 * Verification / Admin Account & Verification Mgmt
 * SLP: Platform Security & Compliance → "Password hashing for all user

 */
@Service
public class AuthService {

    /** HttpSession attribute key holding the admin's user id between MFA step 1 and step 2. */
    private static final String MFA_SESSION_KEY = "MEDIFIND_MFA_PENDING_USER_ID";

    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final NotificationService notificationService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Value("${medifind.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${medifind.security.lockout-minutes:15}")
    private int lockoutMinutes;

    @Value("${medifind.security.mfa-code-valid-minutes:5}")
    private int mfaValidMinutes;

    public AuthService(UserRepository userRepository, PharmacyRepository pharmacyRepository,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.notificationService = notificationService;
    }


    // Registration
    // SLP: Patient Authentication & Profile → "Patient registration with
    // email or phone"


    @Transactional
    public User registerPatient(RegisterPatientRequest request) {
        validatePasswordsMatch(request.getPassword(), request.getConfirmPassword());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("An account with this email already exists.");
        }
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.PATIENT);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    /**
     * SLP: Pharmacy Onboarding & Verification → "Pharmacy registration
     * form", "Add multiple staff logins under one branch account"
     */
    @Transactional
    public User registerPharmacist(RegisterPharmacistRequest request) {
        validatePasswordsMatch(request.getPassword(), request.getConfirmPassword());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("An account with this email already exists.");
        }

        Pharmacy pharmacy;
        if (request.isJoinExisting()) {
            pharmacy = pharmacyRepository.findByLicenseNumber(request.getExistingLicenseNumber())
                    .orElseThrow(() -> new BusinessRuleException("No pharmacy found with that license number."));
        } else {
            if (request.getLicenseNumber() == null || request.getLicenseNumber().isBlank()) {
                throw new BusinessRuleException("License number is required to register a new pharmacy.");
            }
            if (pharmacyRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                throw new BusinessRuleException("A pharmacy with this license number is already registered.");
            }
            pharmacy = new Pharmacy();
            pharmacy.setPharmacyName(request.getPharmacyName());
            pharmacy.setLicenseNumber(request.getLicenseNumber());
            pharmacy.setAddress(request.getAddress());
            pharmacy.setLatitude(request.getLatitude());
            pharmacy.setLongitude(request.getLongitude());
            pharmacy.setPhone(request.getPharmacyPhone());
            pharmacy.setOpeningHours(request.getOpeningHours());
            pharmacy.setVerificationStatus(VerificationStatus.PENDING);
            pharmacy = pharmacyRepository.save(pharmacy);
            notificationService.notifyAdminsOfPharmacyRegistration(pharmacy);
        }

        User staff = new User();
        staff.setFullName(request.getFullName());
        staff.setEmail(request.getEmail());
        staff.setPhone(request.getPhone());
        staff.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        staff.setRole(Role.PHARMACIST);
        staff.setStatus(UserStatus.ACTIVE);
        staff.setPharmacy(pharmacy);
        return userRepository.save(staff);
    }


    // Login — Patient & Pharmacist (single step)
    // SLP: Patient Authentication & Profile → "Secure patient login"
    // SLP: Pharmacy Onboarding & Verification → "Secure pharmacy staff login"


    @Transactional
    public User login(String email, String rawPassword, HttpServletRequest request, HttpServletResponse response) {
        CustomUserDetails principal = authenticate(email, rawPassword);
        if (principal.getUser().getRole() == Role.ADMIN) {
            throw new BusinessRuleException("Admin accounts sign in from the Admin Portal.");
        }
        resetFailedAttempts(principal.getUser());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        establishSecurityContext(authentication, request, response);
        return principal.getUser();
    }


    // Login — Admin (two steps: password, then emailed one-time code)
    // SLP: Admin Account & Verification Mgmt → "Secure admin login with
    // role-based access", "Multi-factor authentication for admin accounts"


    @Transactional
    public void adminLoginStep1(String email, String rawPassword, HttpSession session) {
        CustomUserDetails principal = authenticate(email, rawPassword);
        if (principal.getUser().getRole() != Role.ADMIN) {
            throw new BusinessRuleException("That account is not an admin account.");
        }
        resetFailedAttempts(principal.getUser());

        User admin = principal.getUser();
        String otp = CodeGenerator.sixDigitOtp();
        admin.setMfaCode(otp);
        admin.setMfaCodeExpiry(LocalDateTime.now().plusMinutes(mfaValidMinutes));
        userRepository.save(admin);
        notificationService.sendMfaCode(admin, otp);
        session.setAttribute(MFA_SESSION_KEY, admin.getId());
    }

    @Transactional
    public User completeAdminMfa(String code, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        Object pendingId = session.getAttribute(MFA_SESSION_KEY);
        if (pendingId == null) {
            throw new BusinessRuleException("Your sign-in session expired. Please start again.");
        }
        User admin = userRepository.findById((Long) pendingId)
                .orElseThrow(() -> new BusinessRuleException("That account no longer exists."));

        boolean codeValid = admin.getMfaCode() != null
                && admin.getMfaCode().equals(code == null ? null : code.trim())
                && admin.getMfaCodeExpiry() != null
                && admin.getMfaCodeExpiry().isAfter(LocalDateTime.now());

        if (!codeValid) {
            throw new BusinessRuleException("That code is invalid or has expired. Please try again.");
        }

        admin.setMfaCode(null);
        admin.setMfaCodeExpiry(null);
        userRepository.save(admin);
        session.removeAttribute(MFA_SESSION_KEY);

        CustomUserDetails principal = new CustomUserDetails(admin);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        establishSecurityContext(authentication, request, response);
        return admin;
    }
   // Forgot / reset password — shared by every role
    // SLP: Patient Authentication & Profile → "'Forgot Password' flow via
    // email reset link"


    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = CodeGenerator.urlSafeToken();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            String link = "/auth/reset-password?token=" + token;
            notificationService.sendPasswordResetEmail(user, link);
        });
        // No branch for "email not found" — responding identically either way
        // stops this form being usable to test which emails have an account.
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validatePasswordsMatch(request.getNewPassword(), request.getConfirmPassword());
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new BusinessRuleException("This reset link is invalid or has already been used."));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("This reset link has expired. Please request a new one.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    // Internal helpers

    /**
     * Runs the actual credential check through Spring Security's
     * {@link AuthenticationManager}, which delegates to the auto-configured
     * {@code DaoAuthenticationProvider} — that provider calls
     * {@link com.medifind.security.CustomUserDetailsService} to load the
     * account, compares the password hash, and checks
     * {@code isEnabled()}/{@code isAccountNonLocked()} on our
     * {@link CustomUserDetails}, so all of that logic is written once
     * there rather than duplicated here.
     */
    private CustomUserDetails authenticate(String email, String rawPassword) {
        try {
            Authentication result = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, rawPassword));
            return (CustomUserDetails) result.getPrincipal();
        } catch (BadCredentialsException ex) {
            recordFailedAttempt(email);
            throw new BusinessRuleException("Invalid email or password.");
        } catch (LockedException ex) {
            throw new BusinessRuleException(
                    "This account is temporarily locked due to too many failed attempts. Please try again later.");
        } catch (DisabledException ex) {
            throw new BusinessRuleException("This account has been suspended. Please contact support.");
        }
    }

    /** SLP: Patient Authentication & Profile → "Rate limiting on patient login attempts" */
    private void recordFailedAttempt(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= maxLoginAttempts) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
            }
            userRepository.save(user);
        });
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    private void validatePasswordsMatch(String a, String b) {
        if (a == null || !a.equals(b)) {
            throw new BusinessRuleException("Passwords do not match.");
        }
    }

    /**
     * Manually places the given {@link Authentication} into both the
     * current thread's {@link SecurityContextHolder} (so the rest of THIS
     */
    private void establishSecurityContext(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
