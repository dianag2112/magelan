package bg.softuni.magelan.web;

import bg.softuni.magelan.booking.model.Booking;
import bg.softuni.magelan.booking.service.BookingService;
import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.service.UserService;
import bg.softuni.magelan.web.dto.BookTableRequest;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BookingController {

    private final UserService userService;
    private final BookingService bookingService;

    @GetMapping("/book-table")
    public ModelAndView getBookTablePage(@AuthenticationPrincipal UserData userData) {
        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }

        User user = userService.getById(userData.getUserId());

        List<Booking> upcoming = bookingService.getUpcomingBookings(user.getId());
        List<Booking> past = bookingService.getPastBookingsForUser(user.getId());

        BookTableRequest form = BookTableRequest.builder()
                .date(LocalDate.now())
                .time(LocalTime.of(19, 0))
                .guests(2)
                .phone(user.getPhoneNumber())
                .build();

        ModelAndView modelAndView = new ModelAndView("book-table");
        modelAndView.addObject("bookTableRequest", form);
        modelAndView.addObject("upcomingBookings", upcoming);
        modelAndView.addObject("pastBookings", past);
        return modelAndView;
    }

    @PostMapping("/book-table")
    public ModelAndView bookTable(@AuthenticationPrincipal UserData userData,
                                  @Valid @ModelAttribute("bookTableRequest") BookTableRequest request,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }

        User user = userService.getById(userData.getUserId());

        if (bindingResult.hasErrors()) {
            List<Booking> upcoming = bookingService.getUpcomingBookings(user.getId());
            List<Booking> past = bookingService.getPastBookingsForUser(user.getId());

            ModelAndView modelAndView = new ModelAndView("book-table");
            modelAndView.addObject("bookTableRequest", request);
            modelAndView.addObject("upcomingBookings", upcoming);
            modelAndView.addObject("pastBookings", past);
            return modelAndView;
        }

        bookingService.createBooking(user, request);
        redirectAttributes.addFlashAttribute("message", "Your booking request was submitted.");
        return new ModelAndView("redirect:/book-table");
    }
}
