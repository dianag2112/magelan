package bg.softuni.magelan.booking.repository;

import bg.softuni.magelan.booking.model.Booking;
import bg.softuni.magelan.booking.model.BookingStatus;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("findAllByCustomer_IdAndDateGreaterThanEqualOrderByDateAscTimeAsc returns future & today bookings sorted")
    void findUpcomingBookingsForUser_shouldReturnFutureAndTodaySorted() {
        LocalDate today = LocalDate.of(2025, 1, 1);

        User user = User.builder()
                .username("testUser")
                .password("encoded-pass")
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        entityManager.persist(user);

        Booking pastBooking = Booking.builder()
                .customer(user)
                .date(today.minusDays(1))
                .time(LocalTime.of(18, 0))
                .guests(2)
                .status(BookingStatus.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
        entityManager.persist(pastBooking);

        Booking todayBooking = Booking.builder()
                .customer(user)
                .date(today)
                .time(LocalTime.of(19, 0))
                .guests(2)
                .status(BookingStatus.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
        entityManager.persist(todayBooking);

        Booking tomorrowBooking = Booking.builder()
                .customer(user)
                .date(today.plusDays(1))
                .time(LocalTime.of(20, 0))
                .guests(4)
                .status(BookingStatus.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
        entityManager.persist(tomorrowBooking);

        entityManager.flush();

        var result = bookingRepository
                .findAllByCustomer_IdAndDateGreaterThanEqualOrderByDateAscTimeAsc(
                        user.getId(), today
                );

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Booking::getDate)
                .containsExactly(
                        today,
                        today.plusDays(1)
                );
    }
}
