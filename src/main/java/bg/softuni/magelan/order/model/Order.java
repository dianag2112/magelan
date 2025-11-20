package bg.softuni.magelan.order.model;

import jakarta.persistence.*;
import lombok.*;
import bg.softuni.magelan.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private User customer;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    private String deliveryFullName;

    private String deliveryPhone;

    private String deliveryAddress;

    @Column(length = 1000)
    private String deliveryNotes;

    @Column
    private UUID paymentId;

    @Builder.Default
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdOn ASC")
    private List<OrderItem> items = new ArrayList<>();
}
