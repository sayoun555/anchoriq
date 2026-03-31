package com.anchoriq.api.application.user;

import com.anchoriq.api.dto.request.auth.PasswordChangeRequest;
import com.anchoriq.api.dto.request.auth.ProfileUpdateRequest;
import com.anchoriq.api.dto.response.auth.UserResponse;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.common.exception.InvalidCredentialsException;
import com.anchoriq.core.domain.account.user.model.User;
import com.anchoriq.core.domain.account.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationServiceImpl implements UserApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = findUserById(userId);
        user.updateProfile(request.name());
        User saved = userRepository.save(user);
        log.info("Profile updated: userId={}", userId);
        return UserResponse.from(saved);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.changePassword(encodedNewPassword);
        userRepository.save(user);
        log.info("Password changed: userId={}", userId);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        if (!userRepository.findById(userId).isPresent()) {
            throw new EntityNotFoundException("User", String.valueOf(userId));
        }
        userRepository.deleteById(userId);
        log.info("Account deleted: userId={}", userId);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(userId)));
    }
}
