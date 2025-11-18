package order.repository;

import order.model.Order;
import order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderStatusAndCustomer_Id(OrderStatus status, UUID customerId);

    List<Order> findAllByCustomer_IdAndOrderStatusNotOrderByCreatedOnDesc(UUID customerId,
                                                                          OrderStatus status);
    List<Order> findAllByOrderStatusOrderByCreatedOnDesc(OrderStatus status);
}
