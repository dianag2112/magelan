package bg.softuni.magelan.web;

import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.service.UserService;
import bg.softuni.magelan.web.dto.LoginRequest;
import bg.softuni.magelan.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexControllerTest {

    @Mock
    private UserService userService;

    private IndexController indexController;

    @BeforeEach
    void setUp() {
        indexController = new IndexController(userService);
    }

    @Test
    void getIndexPage_shouldReturnIndexView() {
        String viewName = indexController.getIndexPage();
        assertEquals("index", viewName);
    }

    @Test
    void getLoginPage_shouldReturnLoginViewWithModel() {
        String loginMessage = "Please log in";
        String error = "Bad credentials";

        ModelAndView mav = indexController.getLoginPage(loginMessage, error);

        assertEquals("login", mav.getViewName());
        assertTrue(mav.getModel().containsKey("loginRequest"));
        assertTrue(mav.getModel().get("loginRequest") instanceof LoginRequest);
        assertEquals(loginMessage, mav.getModel().get("loginAttemptMessage"));
        assertEquals(error, mav.getModel().get("error"));
    }

    @Test
    void getRegisterPage_shouldReturnRegisterViewWithEmptyForm() {
        ModelAndView mav = indexController.getRegisterPage();

        assertEquals("register", mav.getViewName());
        assertTrue(mav.getModel().containsKey("registerRequest"));
        assertTrue(mav.getModel().get("registerRequest") instanceof RegisterRequest);
    }

    @Test
    void register_shouldReturnRegisterView_whenValidationErrors() {
        RegisterRequest request = RegisterRequest.builder()
                .username("pirate")
                .password("short")
                .confirmPassword("short")
                .build();

        BindingResult bindingResult = mock(BindingResult.class);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        when(bindingResult.hasErrors()).thenReturn(true);

        ModelAndView mav = indexController.register(request, bindingResult, redirectAttributes);

        assertEquals("register", mav.getViewName());
        assertSame(request, mav.getModel().get("registerRequest"));
        verify(userService, never()).register(any());
    }

    @Test
    void register_shouldRegisterUserAndRedirect_whenNoValidationErrors() {
        RegisterRequest request = RegisterRequest.builder()
                .username("pirate")
                .password("secret123")
                .confirmPassword("secret123")
                .build();

        BindingResult bindingResult = mock(BindingResult.class);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = indexController.register(request, bindingResult, redirectAttributes);

        assertEquals("redirect:/login", mav.getViewName());
        verify(userService).register(request);
    }

    @Test
    void getHomePage_shouldLoadUserAndReturnHomeView() {
        UUID userId = UUID.randomUUID();
        UserData userData = new UserData(userId, "pirate", "pwd", null, true);

        User user = User.builder()
                .id(userId)
                .username("pirate")
                .build();

        when(userService.getById(userId)).thenReturn(user);

        ModelAndView mav = indexController.getHomePage(userData);

        assertEquals("home", mav.getViewName());
        assertSame(user, mav.getModel().get("user"));
        verify(userService).getById(userId);
    }
}
