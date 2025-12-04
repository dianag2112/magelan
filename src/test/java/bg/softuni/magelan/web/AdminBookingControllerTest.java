package bg.softuni.magelan.web;

import bg.softuni.magelan.booking.model.Booking;
import bg.softuni.magelan.booking.model.BookingStatus;
import bg.softuni.magelan.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private AdminBookingController adminBookingController;

    @Test
    void getBookingsPage_shouldPopulateUpcomingAndPastBookings() {
        List<Booking> upcoming = List.of(new Booking());
        List<Booking> past = List.of(new Booking());

        when(bookingService.getUpcomingBookingsForAdmin()).thenReturn(upcoming);
        when(bookingService.getPastBookingsForAdmin()).thenReturn(past);

        ModelAndView mav = adminBookingController.getBookingsPage();

        assertThat(mav.getViewName()).isEqualTo("admin-bookings");
        assertThat(mav.getModel().get("upcomingBookings")).isEqualTo(upcoming);
        assertThat(mav.getModel().get("pastBookings")).isEqualTo(past);
    }

    @Test
    void listBookings_shouldPopulateBookingsAndMessages() {
        List<Booking> bookings = List.of(new Booking());

        when(bookingService.getAllBookings()).thenReturn(bookings);

        ModelAndView mav = adminBookingController.listBookings("ok", "err");

        assertThat(mav.getViewName()).isEqualTo("admin-bookings");
        assertThat(mav.getModel().get("bookings")).isEqualTo(bookings);
        assertThat(mav.getModel().get("message")).isEqualTo("ok");
        assertThat(mav.getModel().get("error")).isEqualTo("err");
    }

    @Test
    void changeStatus_shouldInvokeServiceAndRedirectWithFlashMessage() {
        UUID bookingId = UUID.randomUUID();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = adminBookingController.changeStatus(bookingId, BookingStatus.CONFIRMED, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/bookings");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("message");
        assertThat(redirectAttributes.getFlashAttributes().get("message")).isEqualTo("Status updated!");

        verify(bookingService).changeStatus(bookingId, BookingStatus.CONFIRMED);
    }
}
