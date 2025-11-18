package web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import product.service.ProductService;
import security.UserData;
import user.model.User;
import user.service.UserService;
import web.dto.LoginRequest;
import web.dto.RegisterRequest;

@Controller
public class IndexController {

    private final UserService userService;
    private final ProductService productService;

    @Autowired
    public IndexController(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
    }

    @GetMapping("/")
    public String getIndexPage() {
        return "index";
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(
            @RequestParam(value = "loginAttemptMessage", required = false) String message,
            @RequestParam(value = "error", required = false) String error,
            HttpSession session) {

        ModelAndView modelAndView = new ModelAndView("login");
        modelAndView.addObject("loginRequest", new LoginRequest());
        modelAndView.addObject("loginAttemptMessage", message);
        modelAndView.addObject("error", error);
        return modelAndView;
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage() {
        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());
        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("register");
            modelAndView.addObject("registerRequest", registerRequest);
            return modelAndView;
        }

        userService.register(registerRequest);
        redirectAttributes.addFlashAttribute("successfulRegistration", "You have created your account.");

        return new ModelAndView("redirect:/login");
    }

    @GetMapping("/home")
    public ModelAndView getHomePage(@AuthenticationPrincipal UserData userData) {
        User user = userService.getById(userData.getUserId());
        ModelAndView modelAndView = new ModelAndView("home");
        modelAndView.addObject("user", user);
        return modelAndView;
    }
}
