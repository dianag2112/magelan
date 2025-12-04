package bg.softuni.magelan.booking.service;

import bg.softuni.magelan.booking.model.Booking;
import bg.softuni.magelan.booking.model.BookingStatus;
import bg.softuni.magelan.booking.repository.BookingRepository;
import bg.softuni.magelan.exception.BookingNotFoundException;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.web.dto.BookTableRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    @Transactional
    public void createBooking(User customer, BookTableRequest request) {
        log.info("Creating booking for user {} on {} at {} for {} guests",
                customer.getUsername(), request.getDate(), request.getTime(), request.getGuests());

        Booking booking = Booking.builder()
                .customer(customer)
                .date(request.getDate())
                .time(request.getTime())
                .guests(request.getGuests())
                .phone(request.getPhone())
                .notes(request.getNotes())
                .status(BookingStatus.PENDING)
                .createdOn(LocalDateTime.now())
                .build();

        bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public List<Booking> getUpcomingBookings(UUID userId) {
        LocalDate today = LocalDate.now();
        return bookingRepository
                .findAllByCustomer_IdAndDateGreaterThanEqualOrderByDateAscTimeAsc(userId, today);
    }

    @Transactional(readOnly = true)
    public List<Booking> getPastBookingsForUser(UUID userId) {
        LocalDate today = LocalDate.now();
        return bookingRepository
                .findAllByCustomer_IdAndDateLessThanOrderByDateDesc(userId, today);
    }

    @Transactional(readOnly = true)
    public List<Booking> getUpcomingBookingsForAdmin() {
        LocalDate today = LocalDate.now();
        return bookingRepository
                .findAllByDateGreaterThanEqualOrderByDateAscTimeAsc(today);
    }

    @Transactional(readOnly = true)
    public List<Booking> getPastBookingsForAdmin() {
        LocalDate today = LocalDate.now();
        return bookingRepository
                .findAllByDateLessThanOrderByDateDescTimeDesc(today);
    }

    @Transactional(readOnly = true)
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllByOrderByDateAscTimeAsc();
    }

    @Transactional
    public void changeStatus(UUID bookingId, BookingStatus status) {
        log.info("Changing status of booking {} to {}", bookingId, status);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        booking.setStatus(status);
        bookingRepository.save(booking);

        log.info("Booking {} status changed to {}", bookingId, status);
    }
}
