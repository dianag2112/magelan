package bg.softuni.magelan.web;

import bg.softuni.magelan.order.model.Order;
import bg.softuni.magelan.order.service.OrderService;
import bg.softuni.magelan.order.service.ReceiptService;
import bg.softuni.magelan.product.service.ProductService;
import bg.softuni.magelan.payment.PaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.service.UserService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final UserService userService;
    private final OrderService orderService;
    private final ProductService productService;
    private final ReceiptService receiptService;

    @Autowired
    public OrderController(UserService userService,
                           OrderService orderService,
                           ProductService productService,
                           ReceiptService receiptService) {
        this.userService = userService;
        this.orderService = orderService;
        this.productService = productService;
        this.receiptService = receiptService;
    }

    @GetMapping
    public ModelAndView getOrdersPage(@AuthenticationPrincipal UserData userData) {
        User user = userService.getById(userData.getUserId());

        Order order = orderService
                .findPendingOrderByCustomerId(user.getId())
                .orElseGet(() -> orderService.createPendingOrderForCustomer(user));

        if (order.getPaymentId() != null) {
            return new ModelAndView("redirect:/orders/payment/" + order.getPaymentId());
        }

        List<Order> pastOrders = orderService.getPastOrders(user.getId());

        ModelAndView modelAndView = new ModelAndView("orders");
        modelAndView.addObject("user", user);
        modelAndView.addObject("orderItems", order.getItems());
        modelAndView.addObject("products", orderService.getAvailableProducts());
        modelAndView.addObject("totalAmount", orderService.calculateTotal(order));
        modelAndView.addObject("pastOrders", pastOrders);
        modelAndView.addObject("isAuthenticated", userData != null);
        return modelAndView;
    }


    @PostMapping("/items")
    public String addProductToOrder(@AuthenticationPrincipal UserData userData,
                                    @RequestParam("productId") UUID productId,
                                    @RequestParam("quantity") int quantity,
                                    @RequestParam(value = "returnTo", required = false, defaultValue = "menu") String returnTo,
                                    RedirectAttributes redirectAttributes) {

        if (userData == null) {
            return "redirect:/login";
        }

        User user = userService.getById(userData.getUserId());

        Order pending = orderService.findPendingOrderByCustomerId(user.getId()).orElse(null);
        if (pending != null && pending.getPaymentId() != null) {
            redirectAttributes.addFlashAttribute(
                    "orderMessage",
                    "You have already submitted this order."
            );
            return "redirect:/orders";
        }

        String productName = productService.getById(productId).getName();

        orderService.addProductToCustomerOrder(user, productId, quantity);

        redirectAttributes.addFlashAttribute(
                "orderMessage",
                productName + " added to your order."
        );

        if ("orders".equalsIgnoreCase(returnTo)) {
            return "redirect:/orders";
        } else {
            return "redirect:/menu";
        }
    }


    @DeleteMapping("/items/{itemId}")
    public String removeItem(@AuthenticationPrincipal UserData userData,
                             @PathVariable("itemId") UUID itemId) {

        UUID userId = userData.getUserId();
        orderService.removeItemFromOrder(userId, itemId);

        return "redirect:/orders";
    }

    @PostMapping("/submit")
    public String submitOrder(@AuthenticationPrincipal UserData userData,
                              @RequestParam("fullName") String fullName,
                              @RequestParam("phone") String phone,
                              @RequestParam("address") String address,
                              @RequestParam(value = "notes", required = false) String notes,
                              RedirectAttributes redirectAttributes) {

        if (userData == null) {
            return "redirect:/login";
        }

        UUID userId = userData.getUserId();

        PaymentResponse payment = orderService.startPaymentForCurrentOrder(
                userId,
                fullName,
                phone,
                address,
                notes
        );

        redirectAttributes.addFlashAttribute(
                "orderMessage",
                "Your order is ready. Please complete the payment."
        );

        return "redirect:/orders/payment/" + payment.getId();
    }

    @GetMapping("/payment/{paymentId}")
    public ModelAndView showPaymentPage(@PathVariable("paymentId") UUID paymentId,
                                        @AuthenticationPrincipal UserData userData) {

        PaymentResponse payment = orderService.getPaymentById(paymentId);

        ModelAndView modelAndView = new ModelAndView("payment");
        modelAndView.addObject("payment", payment);
        modelAndView.addObject("isAuthenticated", userData != null);
        return modelAndView;
    }

    @PostMapping("/payment/{paymentId}/process")
    public String processPayment(@PathVariable("paymentId") UUID paymentId,
                                 RedirectAttributes redirectAttributes) {

        PaymentResponse updated = orderService.processPayment(paymentId);

        if ("PAID".equalsIgnoreCase(updated.getStatus())) {
            redirectAttributes.addFlashAttribute(
                    "orderMessage",
                    "Payment completed. Thank you! Your order was submitted."
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "orderMessage",
                    "Payment status: " + updated.getStatus()
            );
        }

        return "redirect:/orders";
    }

    @GetMapping("/{orderId}")
    public ModelAndView viewOrder(@AuthenticationPrincipal UserData userData,
                                  @PathVariable("orderId") UUID orderId) {
        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }

        User user = userService.getById(userData.getUserId());
        Order order = orderService.getOrderById(orderId);

        if (!order.getCustomer().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not allowed to view this order.");
        }

        PaymentResponse payment = orderService.getPaymentForOrder(orderId);

        ModelAndView modelAndView = new ModelAndView("order-details-user");
        modelAndView.addObject("order", order);
        modelAndView.addObject("payment", payment);
        return modelAndView;
    }

    @PostMapping("/cancel")
    public String cancelOrder(@AuthenticationPrincipal UserData userData,
                              @RequestParam("orderId") UUID orderId,
                              RedirectAttributes redirectAttributes) {

        if (userData == null) {
            return "redirect:/login";
        }

        orderService.cancelOrder(orderId, userData.getUserId());
        redirectAttributes.addFlashAttribute("orderMessage", "Order has been cancelled.");

        return "redirect:/orders";
    }

    @ResponseBody
    @GetMapping("/{orderId}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable UUID orderId,
                                                  @AuthenticationPrincipal UserData userData) {

        Order order = orderService.getOrderById(orderId);

        if (!order.getCustomer().getId().equals(userData.getUserId())
                && !userData.getRole().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        byte[] pdf = receiptService.generateReceiptPdf(order);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=receipt-" + orderId + ".pdf")
                .body(pdf);
    }
}
