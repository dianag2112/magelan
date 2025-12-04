package bg.softuni.magelan.booking.service;

import bg.softuni.magelan.booking.model.Booking;
import bg.softuni.magelan.booking.model.BookingStatus;
import bg.softuni.magelan.booking.repository.BookingRepository;
import bg.softuni.magelan.exception.BookingNotFoundException;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.web.dto.BookTableRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private BookTableRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("pirateUser");

        request = BookTableRequest.builder()
                .date(LocalDate.of(2025, 12, 1))
                .time(LocalTime.of(19, 30))
                .guests(4)
                .phone("0888123456")
                .notes("Near the window")
                .build();
    }

    @Test
    void createBooking_shouldSaveBookingWithCorrectFields() {
        bookingService.createBooking(user, request);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());

        Booking saved = captor.getValue();
        assertThat(saved.getCustomer()).isEqualTo(user);
        assertThat(saved.getDate()).isEqualTo(request.getDate());
        assertThat(saved.getTime()).isEqualTo(request.getTime());
        assertThat(saved.getGuests()).isEqualTo(request.getGuests());
        assertThat(saved.getPhone()).isEqualTo(request.getPhone());
        assertThat(saved.getNotes()).isEqualTo(request.getNotes());
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(saved.getCreatedOn()).isNotNull();
        assertThat(saved.getCreatedOn()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void getUpcomingBookings_shouldDelegateToRepositoryWithToday() {
        UUID userId = UUID.randomUUID();
        List<Booking> bookings = List.of(new Booking());
        when(bookingRepository
                .findAllByCustomer_IdAndDateGreaterThanEqualOrderByDateAscTimeAsc(eq(userId), any(LocalDate.class)))
                .thenReturn(bookings);

        List<Booking> result = bookingService.getUpcomingBookings(userId);

        assertThat(result).isEqualTo(bookings);
        verify(bookingRepository)
                .findAllByCustomer_IdAndDateGreaterThanEqualOrderByDateAscTimeAsc(eq(userId), any(LocalDate.class));
    }

    @Test
    void getPastBookingsForUser_shouldDelegateToRepositoryWithToday() {
        UUID userId = UUID.randomUUID();
        List<Booking> bookings = List.of(new Booking());
        when(bookingRepository
                .findAllByCustomer_IdAndDateLessThanOrderByDateDesc(eq(userId), any(LocalDate.class)))
                .thenReturn(bookings);

        List<Booking> result = bookingService.getPastBookingsForUser(userId);

        assertThat(result).isEqualTo(bookings);
        verify(bookingRepository)
                .findAllByCustomer_IdAndDateLessThanOrderByDateDesc(eq(userId), any(LocalDate.class));
    }

    @Test
    void getUpcomingBookingsForAdmin_shouldDelegateToRepository() {
        List<Booking> bookings = List.of(new Booking());
        when(bookingRepository
                .findAllByDateGreaterThanEqualOrderByDateAscTimeAsc(any(LocalDate.class)))
                .thenReturn(bookings);

        List<Booking> result = bookingService.getUpcomingBookingsForAdmin();

        assertThat(result).isEqualTo(bookings);
        verify(bookingRepository)
                .findAllByDateGreaterThanEqualOrderByDateAscTimeAsc(any(LocalDate.class));
    }

    @Test
    void getPastBookingsForAdmin_shouldDelegateToRepository() {
        List<Booking> bookings = List.of(new Booking());
        when(bookingRepository
                .findAllByDateLessThanOrderByDateDescTimeDesc(any(LocalDate.class)))
                .thenReturn(bookings);

        List<Booking> result = bookingService.getPastBookingsForAdmin();

        assertThat(result).isEqualTo(bookings);
        verify(bookingRepository)
                .findAllByDateLessThanOrderByDateDescTimeDesc(any(LocalDate.class));
    }

    @Test
    void getAllBookings_shouldDelegateToRepository() {
        List<Booking> bookings = List.of(new Booking());
        when(bookingRepository.findAllByOrderByDateAscTimeAsc()).thenReturn(bookings);

        List<Booking> result = bookingService.getAllBookings();

        assertThat(result).isEqualTo(bookings);
        verify(bookingRepository).findAllByOrderByDateAscTimeAsc();
    }

    @Test
    void changeStatus_shouldUpdateStatusAndSave_whenBookingExists() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        bookingService.changeStatus(bookingId, BookingStatus.CONFIRMED);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void changeStatus_shouldThrowBookingNotFoundException_whenBookingMissing() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class,
                () -> bookingService.changeStatus(bookingId, BookingStatus.CANCELLED));

        verify(bookingRepository).findById(bookingId);
        verify(bookingRepository, never()).save(any());
    }
}
