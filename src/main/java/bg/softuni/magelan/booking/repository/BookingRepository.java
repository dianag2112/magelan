package bg.softuni.magelan.booking.repository;

import bg.softuni.magelan.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findAllByCustomer_IdAndDateGreaterThanEqualOrderByDateAscTimeAsc(
            UUID customerId,
            LocalDate date
    );

    List<Booking> findAllByCustomer_IdAndDateLessThanOrderByDateDesc(
            UUID customerId,
            LocalDate date
    );

    List<Booking> findAllByOrderByDateAscTimeAsc();

    List<Booking> findAllByDateGreaterThanEqualOrderByDateAscTimeAsc(LocalDate dateFrom);

    List<Booking> findAllByDateLessThanOrderByDateDescTimeDesc(LocalDate dateTo);
}
