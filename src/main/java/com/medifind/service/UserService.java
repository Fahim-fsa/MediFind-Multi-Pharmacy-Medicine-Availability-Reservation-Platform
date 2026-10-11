package com.medifind.service;

import com.medifind.entity.User;
import com.medifind.enums.Role;
import com.medifind.enums.UserStatus;
import com.medifind.exception.ResourceNotFoundException;
import com.medifind.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SLP: Patient Authentication & Profile → "Patient profile view & edit"
 * SLP: Admin Account & Verification Mgmt → "View and suspend patient accounts"
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional
    public User updateProfile(Long userId, String fullName, String phone) {
        User user = getById(userId);
        user.setFullName(fullName);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    /** SLP: Admin Reporting & Configuration → "Sortable/filterable/exportable admin data tables" */
    public Page<User> searchPatients(UserStatus status, String search, Pageable pageable) {
        return userRepository.search(Role.PATIENT, status, search, pageable);
    }

    @Transactional
    public User setStatus(Long userId, UserStatus status) {
        User user = getById(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }

    public long countActivePatients() {
        return userRepository.countByRoleAndStatus(Role.PATIENT, UserStatus.ACTIVE);
    }

    public long countTotalPatients() {
        return userRepository.countByRole(Role.PATIENT);
    }
}
