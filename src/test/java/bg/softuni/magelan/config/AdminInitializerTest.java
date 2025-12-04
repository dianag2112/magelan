package bg.softuni.magelan.config;

import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.model.UserRole;
import bg.softuni.magelan.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminInitializer adminInitializer;

    @Test
    void run_shouldCreateAdmin_whenNoAdminExists() throws Exception {

        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(0L);
        when(passwordEncoder.encode("admin123")).thenReturn("encoded-pass");

        adminInitializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getPassword()).isEqualTo("encoded-pass");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCreatedOn()).isNotNull();
        assertThat(saved.getUpdatedOn()).isNotNull();
    }

    @Test
    void run_shouldDoNothing_whenAdminAlreadyExists() throws Exception {

        when(userRepository.countByRole(UserRole.ADMIN)).thenReturn(1L);

        adminInitializer.run();

        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository);
    }
}
