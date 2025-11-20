package bg.softuni.magelan.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.service.UserService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPanelController {

    private final UserService userService;

    @GetMapping("/panel")
    public ModelAndView getAdminPanel(@AuthenticationPrincipal UserData userData) {
        List<User> users = userService.getAll();

        ModelAndView modelAndView = new ModelAndView("admin-panel");
        modelAndView.addObject("users", users);
        modelAndView.addObject("currentUserId", userData.getUserId());
        return modelAndView;
    }

    @PostMapping("/users/{userId}/role")
    public String changeUserRole(@PathVariable("userId") UUID userId,
                                 RedirectAttributes redirectAttributes) {
        userService.switchRole(userId);
        redirectAttributes.addFlashAttribute("message", "User role updated.");
        return "redirect:/admin/panel";
    }

    @PostMapping("/users/{userId}/active")
    public String toggleActive(@PathVariable("userId") UUID userId,
                               RedirectAttributes redirectAttributes) {
        userService.toggleActive(userId);
        redirectAttributes.addFlashAttribute("message", "User status updated.");
        return "redirect:/admin/panel";
    }
}
