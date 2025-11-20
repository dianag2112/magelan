package bg.softuni.magelan.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.service.UserService;
import bg.softuni.magelan.web.dto.EditProfileRequest;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    public ModelAndView getProfilePage(@AuthenticationPrincipal UserData userData) {
        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }

        User user = userService.getById(userData.getUserId());

        ModelAndView modelAndView = new ModelAndView("profile");
        modelAndView.addObject("user", user);
        return modelAndView;
    }

    @GetMapping("/profile/settings")
    public ModelAndView getEditProfilePage(@AuthenticationPrincipal UserData userData) {
        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }

        User user = userService.getById(userData.getUserId());

        EditProfileRequest form = EditProfileRequest.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .profilePictureUrl(user.getProfilePicture())
                .build();

        ModelAndView modelAndView = new ModelAndView("profile-edit");
        modelAndView.addObject("editProfileRequest", form);
        return modelAndView;
    }

    @PostMapping("/profile")
    public ModelAndView editProfile(@AuthenticationPrincipal UserData userData,
                                    @Valid @ModelAttribute("editProfileRequest") EditProfileRequest editProfileRequest,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("profile-edit");
            modelAndView.addObject("editProfileRequest", editProfileRequest);
            return modelAndView;
        }

        userService.updateProfile(userData.getUserId(), editProfileRequest);
        redirectAttributes.addFlashAttribute("profileUpdated", "Profile updated successfully.");

        return new ModelAndView("redirect:/profile");
    }
}
