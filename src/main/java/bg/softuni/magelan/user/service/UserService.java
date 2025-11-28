package bg.softuni.magelan.user.service;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import bg.softuni.magelan.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.model.UserRole;
import bg.softuni.magelan.user.repository.UserRepository;
import bg.softuni.magelan.web.dto.EditProfileRequest;
import bg.softuni.magelan.web.dto.RegisterRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User register(RegisterRequest registerRequest) {
        log.info("Registering new user with username {}", registerRequest.getUsername());

        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        log.info("User {} registered successfully with ID {}", registerRequest.getUsername(), user.getId());
        return user;
    }

    @Cacheable("users")
    public List<User> getAll() {
        log.debug("Fetching all users (may be served from cache).");
        return userRepository.findAll();
    }

    public User getByUsername(String username) {
        log.debug("Fetching user by username {}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User with username {} not found.", username);
                    return new RuntimeException("Username [%s] is not found.".formatted(username));
                });
    }

    public User getById(UUID id) {
        log.debug("Fetching user by ID {}", id);

        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User with ID {} not found.", id);
                    return new RuntimeException("User with ID [%s] is not found.".formatted(id));
                });
    }

    @CacheEvict(value = "users", allEntries = true)
    public void updateProfile(UUID id, EditProfileRequest editProfileRequest) {
        log.info("Updating profile for user {}", id);

        User user = getById(id);

        user.setFirstName(editProfileRequest.getFirstName());
        user.setLastName(editProfileRequest.getLastName());
        user.setEmail(editProfileRequest.getEmail());
        user.setPhoneNumber(editProfileRequest.getPhoneNumber());
        user.setAddress(editProfileRequest.getAddress());
        user.setProfilePicture(editProfileRequest.getProfilePictureUrl());
        user.setUpdatedOn(LocalDateTime.now());

        userRepository.save(user);

        log.info("Profile updated successfully for user {}", id);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void switchRole(UUID id) {
        log.info("Switching role for user {}", id);

        User user = getById(id);

        if (user.getRole() == UserRole.ADMIN) {
            long adminCount = userRepository.countByRoleAndActiveTrue(UserRole.ADMIN);
            log.debug("Current active admin count: {}", adminCount);

            if (adminCount <= 1) {
                log.warn("Attempted to remove role ADMIN from the last active admin (user {}).", id);
                throw new IllegalStateException("Cannot remove the last admin user.");
            }
            user.setRole(UserRole.USER);
            log.info("User {} role changed from ADMIN to USER", id);
        } else {
            user.setRole(UserRole.ADMIN);
            log.info("User {} role changed from USER to ADMIN", id);
        }

        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);

        log.info("Role switch persisted for user {}", id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for authentication: {}", username);

        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession currentSession = servletRequestAttributes.getRequest().getSession(true);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Authentication failed. Username {} not found.", username);
                    return new UsernameNotFoundException("Username [%s] is not found.".formatted(username));
                });

        log.info("User {} authenticated successfully (ID: {}, role: {}, active: {}).",
                username, user.getId(), user.getRole(), user.isActive());

        return new UserData(user.getId(), username, user.getPassword(), user.getRole(), user.isActive());
    }

    @CacheEvict(value = "users", allEntries = true)
    public void toggleActive(UUID userId) {
        log.info("Toggling active status for user {}", userId);

        User user = getById(userId);

        if (user.getRole() == UserRole.ADMIN && user.isActive()) {
            long activeAdminCount = userRepository.countByRoleAndActiveTrue(UserRole.ADMIN);
            log.debug("Active admin count before deactivation attempt: {}", activeAdminCount);

            if (activeAdminCount <= 1) {
                log.warn("Attempt to deactivate last active admin (user {}). Operation blocked.", userId);
                throw new IllegalStateException("Cannot deactivate the last active admin.");
            }
        }

        user.setActive(!user.isActive());
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} is now {}", userId, user.isActive() ? "ACTIVE" : "INACTIVE");
    }

    private long getAdminCount() {
        long count = userRepository.countByRoleAndActiveTrue(UserRole.ADMIN);
        log.debug("Current active admin count: {}", count);
        return count;
    }
}
