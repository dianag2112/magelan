package bg.softuni.magelan.web;

import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.model.UserRole;
import bg.softuni.magelan.user.service.UserService;
import bg.softuni.magelan.web.dto.EditProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private ProfileController profileController;

    private UUID userId;
    private User user;
    private UserData userData;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .username("pirate")
                .firstName("Jack")
                .lastName("Sparrow")
                .email("jack@ship.com")
                .phoneNumber("123456")
                .address("The Black Pearl")
                .profilePicture("http://image")
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        userData = new UserData(
                userId,
                "pirate",
                "encoded",
                UserRole.USER,
                true
        );
    }

    @Test
    void getProfilePage_shouldRedirectToLogin_whenUserNotAuthenticated() {
        ModelAndView mav = profileController.getProfilePage(null);

        assertThat(mav.getViewName()).isEqualTo("redirect:/login");
        verifyNoInteractions(userService);
    }

    @Test
    void getProfilePage_shouldReturnProfileViewWithUser_whenAuthenticated() {
        when(userService.getById(userId)).thenReturn(user);

        ModelAndView mav = profileController.getProfilePage(userData);

        assertThat(mav.getViewName()).isEqualTo("profile");
        assertThat(mav.getModel()).containsKey("user");
        assertThat(mav.getModel().get("user")).isEqualTo(user);

        verify(userService).getById(userId);
    }

    @Test
    void getEditProfilePage_shouldRedirectToLogin_whenUserNotAuthenticated() {
        ModelAndView mav = profileController.getEditProfilePage(null);

        assertThat(mav.getViewName()).isEqualTo("redirect:/login");
        verifyNoInteractions(userService);
    }

    @Test
    void getEditProfilePage_shouldReturnEditViewWithForm_whenAuthenticated() {
        when(userService.getById(userId)).thenReturn(user);

        ModelAndView mav = profileController.getEditProfilePage(userData);

        assertThat(mav.getViewName()).isEqualTo("profile-edit");
        assertThat(mav.getModel()).containsKey("editProfileRequest");

        EditProfileRequest form = (EditProfileRequest) mav.getModel().get("editProfileRequest");
        assertThat(form.getFirstName()).isEqualTo("Jack");
        assertThat(form.getLastName()).isEqualTo("Sparrow");
        assertThat(form.getEmail()).isEqualTo("jack@ship.com");
        assertThat(form.getPhoneNumber()).isEqualTo("123456");
        assertThat(form.getAddress()).isEqualTo("The Black Pearl");
        assertThat(form.getProfilePictureUrl()).isEqualTo("http://image");

        verify(userService).getById(userId);
    }

    @Test
    void editProfile_shouldRedirectToLogin_whenUserNotAuthenticated() {
        ModelAndView mav = profileController.editProfile(
                null,
                EditProfileRequest.builder().build(),
                bindingResult,
                redirectAttributes
        );

        assertThat(mav.getViewName()).isEqualTo("redirect:/login");
        verifyNoInteractions(userService);
    }

    @Test
    void editProfile_shouldReturnEditView_whenValidationErrors() {
        EditProfileRequest req = EditProfileRequest.builder()
                .firstName("New")
                .build();

        when(bindingResult.hasErrors()).thenReturn(true);

        ModelAndView mav = profileController.editProfile(
                userData,
                req,
                bindingResult,
                redirectAttributes
        );

        assertThat(mav.getViewName()).isEqualTo("profile-edit");
        assertThat(mav.getModel().get("editProfileRequest")).isEqualTo(req);

        verifyNoInteractions(userService);
    }

    @Test
    void editProfile_shouldUpdateProfileAndRedirect_whenValid() {
        EditProfileRequest req = EditProfileRequest.builder()
                .firstName("New")
                .lastName("Name")
                .email("new@ship.com")
                .phoneNumber("999999")
                .address("New Harbor")
                .profilePictureUrl("http://pic")
                .build();

        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = profileController.editProfile(
                userData,
                req,
                bindingResult,
                redirectAttributes
        );

        assertThat(mav.getViewName()).isEqualTo("redirect:/profile");
        verify(userService).updateProfile(userId, req);
        verify(redirectAttributes)
                .addFlashAttribute("profileUpdated", "Profile updated successfully.");
    }
}
