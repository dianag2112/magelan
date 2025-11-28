package bg.softuni.magelan.web;

import bg.softuni.magelan.booking.model.Booking;
import bg.softuni.magelan.booking.model.BookingStatus;
import bg.softuni.magelan.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    public ModelAndView getBookingsPage() {
        ModelAndView mav = new ModelAndView("admin-bookings");
        mav.addObject("upcomingBookings", bookingService.getUpcomingBookingsForAdmin());
        mav.addObject("pastBookings", bookingService.getPastBookingsForAdmin());
        return mav;
    }

    @GetMapping("/search")
    public ModelAndView listBookings(@ModelAttribute("message") String message,
                                     @ModelAttribute("error") String error) {

        List<Booking> bookings = bookingService.getAllBookings();

        ModelAndView mav = new ModelAndView("admin-bookings");
        mav.addObject("bookings", bookings);
        mav.addObject("message", message);
        mav.addObject("error", error);
        return mav;
    }

    @PostMapping("{id}/status")
    public String changeStatus(
            @PathVariable UUID id,
            @RequestParam BookingStatus status,
            RedirectAttributes redirectAttributes) {

        bookingService.changeStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Status updated!");

        return "redirect:/admin/bookings";
    }
}
