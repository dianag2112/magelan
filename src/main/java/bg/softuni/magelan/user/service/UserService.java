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
    private final OrderService orderService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, OrderService orderService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderService = orderService;
    }
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public User register(RegisterRequest registerRequest) {
        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        log.info("user [%s] is registered.".formatted(registerRequest.getUsername()));
        return user;
    }
    @Cacheable("users")
    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Username [%s] is not found.".formatted(username)));
    }

    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User with ID [%s] is not found.".formatted(id)));
    }

    @CacheEvict(value = "users", allEntries = true)
    public void updateProfile(UUID id, EditProfileRequest editProfileRequest) {
        User user = getById(id);

        user.setFirstName(editProfileRequest.getFirstName());
        user.setLastName(editProfileRequest.getLastName());
        user.setEmail(editProfileRequest.getEmail());
        user.setPhoneNumber(editProfileRequest.getPhoneNumber());
        user.setAddress(editProfileRequest.getAddress());
        user.setProfilePicture(editProfileRequest.getProfilePictureUrl());

        userRepository.save(user);
    }

    @CacheEvict(value = "users", allEntries = true)
    public void switchRole(UUID id) {
        User user = getById(id);

        if (user.getRole() == UserRole.ADMIN) {
            long adminCount = userRepository.countByRoleAndActiveTrue(UserRole.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot remove the last admin user.");
            }
            user.setRole(UserRole.USER);
        } else {
            user.setRole(UserRole.ADMIN);
        }

        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession currentSession = servletRequestAttributes.getRequest().getSession(true);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Username [%s] is not found.".formatted(username)));

        return new UserData(user.getId(), username, user.getPassword(), user.getRole(), user.isActive());
    }

    public void toggleActive(UUID userId) {
        User user = getById(userId);

        if (user.getRole() == UserRole.ADMIN) {

            if (user.isActive()) {
                long activeAdminCount = userRepository.countByRoleAndActiveTrue(UserRole.ADMIN);

                if (activeAdminCount <= 1) {
                    throw new IllegalStateException("Cannot deactivate the last active admin.");
                }
            }
        }

        user.setActive(!user.isActive());
        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }


    private long getAdminCount() {
        return userRepository.countByRoleAndActiveTrue(UserRole.ADMIN);
    }
}


