package bg.softuni.magelan.order.repository;

import bg.softuni.magelan.order.model.Order;
import bg.softuni.magelan.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderStatusAndCustomer_Id(OrderStatus status, UUID customerId);

    List<Order> findAllByCustomer_IdAndOrderStatusNotOrderByCreatedOnDesc(UUID customerId,
                                                                          OrderStatus status);
    List<Order> findAllByOrderStatusOrderByCreatedOnDesc(OrderStatus status);

    Optional<Order> findByPaymentId(UUID paymentId);
}
