package bg.softuni.magelan.web;

import bg.softuni.magelan.booking.model.Booking;
import bg.softuni.magelan.booking.service.BookingService;
import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.service.UserService;
import bg.softuni.magelan.web.dto.BookTableRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    @Mock
    private UserData userData;

    private User user;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setUsername("pirate");
        user.setPhoneNumber("0888123456");

        lenient().when(userData.getUserId()).thenReturn(userId);
        lenient().when(userService.getById(userId)).thenReturn(user);
    }

    @Test
    void getBookTablePage_shouldRedirectToLogin_whenUserNotAuthenticated() {
        ModelAndView mav = bookingController.getBookTablePage(null);

        assertThat(mav.getViewName()).isEqualTo("redirect:/login");
    }

    @Test
    void getBookTablePage_shouldReturnViewWithFormAndBookings_whenUserAuthenticated() {

        when(userService.getById(user.getId())).thenReturn(user);

        List<Booking> upcoming = List.of(new Booking());
        List<Booking> past = List.of(new Booking());

        when(bookingService.getUpcomingBookings(user.getId()))
                .thenReturn(upcoming);

        when(bookingService.getPastBookingsForUser(user.getId()))
                .thenReturn(past);

        ModelAndView mav = bookingController.getBookTablePage(userData);

        assertThat(mav.getViewName()).isEqualTo("book-table");
        assertThat(mav.getModel()).containsKeys("bookTableRequest", "upcomingBookings", "pastBookings");

        BookTableRequest form = (BookTableRequest) mav.getModel().get("bookTableRequest");
        assertThat(form.getDate()).isEqualTo(LocalDate.now());
        assertThat(form.getTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(form.getGuests()).isEqualTo(2);
        assertThat(form.getPhone()).isEqualTo(user.getPhoneNumber());

        assertThat(mav.getModel().get("upcomingBookings")).isEqualTo(upcoming);
        assertThat(mav.getModel().get("pastBookings")).isEqualTo(past);
    }

    @Test
    void bookTable_shouldRedirectToLogin_whenUserNotAuthenticated() {
        BookTableRequest request = BookTableRequest.builder().build();
        BindingResult result = new BeanPropertyBindingResult(request, "bookTableRequest");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView mav = bookingController.bookTable(null, request, result, redirectAttributes);

        assertThat(mav.getViewName()).isEqualTo("redirect:/login");
    }

    @Test
    void bookTable_shouldReturnFormWithErrors_whenBindingHasErrors() {
        BookTableRequest request = BookTableRequest.builder()
                .date(LocalDate.now())
                .time(LocalTime.of(18, 0))
                .guests(0)
                .build();

        BindingResult result = new BeanPropertyBindingResult(request, "bookTableRequest");
        result.rejectValue("guests", "guests.min", "Guests must be at least 1");

        List<Booking> upcoming = List.of(new Booking());
        List<Booking> past = List.of(new Booking());
        when(bookingService.getUpcomingBookings(user.getId())).thenReturn(upcoming);
        when(bookingService.getPastBookingsForUser(user.getId())).thenReturn(past);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView mav = bookingController.bookTable(userData, request, result, redirectAttributes);

        assertThat(mav.getViewName()).isEqualTo("book-table");
        assertThat(mav.getModel().get("bookTableRequest")).isEqualTo(request);
        assertThat(mav.getModel().get("upcomingBookings")).isEqualTo(upcoming);
        assertThat(mav.getModel().get("pastBookings")).isEqualTo(past);

        verify(bookingService, never()).createBooking(any(), any());
    }

    @Test
    void bookTable_shouldCreateBookingAndRedirect_whenValid() {
        BookTableRequest request = BookTableRequest.builder()
                .date(LocalDate.now())
                .time(LocalTime.of(19, 0))
                .guests(2)
                .phone("0888123456")
                .notes("Window seat")
                .build();

        BindingResult result = new BeanPropertyBindingResult(request, "bookTableRequest");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        ModelAndView mav = bookingController.bookTable(userData, request, result, redirectAttributes);

        assertThat(mav.getViewName()).isEqualTo("redirect:/book-table");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("message");
        assertThat(redirectAttributes.getFlashAttributes().get("message"))
                .isEqualTo("Your booking request was submitted.");

        verify(bookingService).createBooking(user, request);
    }
}
