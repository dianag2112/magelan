package bg.softuni.magelan.user.service;

import bg.softuni.magelan.exception.UserNotFoundException;
import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.model.UserRole;
import bg.softuni.magelan.user.repository.UserRepository;
import bg.softuni.magelan.web.dto.EditProfileRequest;
import bg.softuni.magelan.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("pirate")
                .password("encoded-pass")
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    @Test
    void register_shouldEncodePasswordAndSaveUser() {
        RegisterRequest req = RegisterRequest.builder()
                .username("newUser")
                .password("raw-pass")
                .build();

        when(passwordEncoder.encode("raw-pass")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User saved = invocation.getArgument(0);
                    saved.setId(UUID.randomUUID());
                    return saved;
                });

        userService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("newUser");
        assertThat(saved.getPassword()).isEqualTo("encoded-pass");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedOn()).isNotNull();
        assertThat(saved.getUpdatedOn()).isNotNull();
    }

    @Test
    void getAll_shouldDelegateToRepository() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> result = userService.getAll();

        assertThat(result).containsExactly(user);
        verify(userRepository).findAll();
    }

    @Test
    void getById_shouldReturnUser_whenExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getById(userId);

        assertThat(result).isEqualTo(user);
        verify(userRepository).findById(userId);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(userId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
    }

    @Test
    void updateProfile_shouldUpdateFieldsAndSave() {
        EditProfileRequest req = EditProfileRequest.builder()
                .firstName("Jack")
                .lastName("Sparrow")
                .email("jack@ship.com")
                .phoneNumber("123456")
                .address("The Black Pearl")
                .profilePictureUrl("http://image")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateProfile(userId, req);

        assertThat(user.getFirstName()).isEqualTo("Jack");
        assertThat(user.getLastName()).isEqualTo("Sparrow");
        assertThat(user.getEmail()).isEqualTo("jack@ship.com");
        assertThat(user.getPhoneNumber()).isEqualTo("123456");
        assertThat(user.getAddress()).isEqualTo("The Black Pearl");
        assertThat(user.getProfilePicture()).isEqualTo("http://image");
        assertThat(user.getUpdatedOn()).isNotNull();

        verify(userRepository).save(user);
    }

    @Test
    void switchRole_shouldChangeAdminToUser_whenMoreAdminsExist() {
        user.setRole(UserRole.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN))
                .thenReturn(2L);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.switchRole(userId);

        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        verify(userRepository).save(user);
    }

    @Test
    void switchRole_shouldThrow_whenLastActiveAdmin() {
        user.setRole(UserRole.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN))
                .thenReturn(1L); // last admin

        assertThatThrownBy(() -> userService.switchRole(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot remove the last admin user");

        verify(userRepository, never()).save(any());
    }

    @Test
    void switchRole_shouldChangeUserToAdmin() {
        user.setRole(UserRole.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.switchRole(userId);

        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    void loadUserByUsername_shouldReturnUserData_whenUserExists() {
        when(userRepository.findByUsername("pirate"))
                .thenReturn(Optional.of(user));

        var details = userService.loadUserByUsername("pirate");

        assertThat(details).isInstanceOf(UserData.class);
        UserData userData = (UserData) details;
        assertThat(userData.getUsername()).isEqualTo("pirate");
        assertThat(userData.getUserId()).isEqualTo(userId);
        assertThat(userData.getAuthorities()).hasSize(1);
        assertThat(userData.isEnabled()).isEqualTo(user.isActive());

        verify(userRepository).findByUsername("pirate");
    }

    @Test
    void loadUserByUsername_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("ghost"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByUsername("ghost");
    }

    @Test
    void toggleActive_shouldFlipActiveFlagForUser() {
        user.setRole(UserRole.USER);
        user.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userService.toggleActive(userId);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void toggleActive_shouldThrow_whenLastActiveAdmin() {
        user.setRole(UserRole.ADMIN);
        user.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN))
                .thenReturn(1L);

        assertThatThrownBy(() -> userService.toggleActive(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot deactivate the last active admin");

        verify(userRepository, never()).save(any());
    }
}
