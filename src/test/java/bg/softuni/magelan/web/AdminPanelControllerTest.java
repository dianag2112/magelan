package bg.softuni.magelan.web;

import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.model.UserRole;
import bg.softuni.magelan.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPanelControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AdminPanelController adminPanelController;

    private UUID adminId;
    private User adminUser;
    private UserData adminData;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        adminUser = User.builder()
                .id(adminId)
                .username("admin")
                .role(UserRole.ADMIN)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        adminData = new UserData(
                adminId,
                "admin",
                "encoded",
                UserRole.ADMIN,
                true
        );
    }

    @Test
    void getAdminPanel_shouldReturnViewWithUsersAndCurrentUserId() {
        when(userService.getAll()).thenReturn(List.of(adminUser));

        ModelAndView mav = adminPanelController.getAdminPanel(adminData);

        assertThat(mav.getViewName()).isEqualTo("admin-panel");
        assertThat(mav.getModel()).containsKeys("users", "currentUserId");

        @SuppressWarnings("unchecked")
        List<User> users = (List<User>) mav.getModel().get("users");
        assertThat(users).containsExactly(adminUser);

        assertThat(mav.getModel().get("currentUserId")).isEqualTo(adminId);
        verify(userService).getAll();
    }

    @Test
    void changeUserRole_shouldCallServiceAndRedirect() {
        UUID targetUserId = UUID.randomUUID();

        String view = adminPanelController.changeUserRole(targetUserId, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/panel");
        verify(userService).switchRole(targetUserId);
        verify(redirectAttributes).addFlashAttribute("message", "User role updated.");
    }

    @Test
    void toggleActive_shouldCallServiceAndRedirect() {
        UUID targetUserId = UUID.randomUUID();

        String view = adminPanelController.toggleActive(targetUserId, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/panel");
        verify(userService).toggleActive(targetUserId);
        verify(redirectAttributes).addFlashAttribute("message", "User status updated.");
    }
}
