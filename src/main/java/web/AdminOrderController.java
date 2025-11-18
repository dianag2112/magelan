package web;

import lombok.RequiredArgsConstructor;
import order.model.Order;
import order.model.OrderStatus;
import order.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ModelAndView getAdminOrdersPage(
            @ModelAttribute("message") String message,
            @ModelAttribute("error") String error) {

        List<Order> submitted = orderService.getOrdersByStatus(OrderStatus.SUBMITTED);
        List<Order> confirmed = orderService.getOrdersByStatus(OrderStatus.CONFIRMED);
        List<Order> delivered = orderService.getOrdersByStatus(OrderStatus.DELIVERED);

        ModelAndView modelAndView = new ModelAndView("admin-orders");
        modelAndView.addObject("submittedOrders", submitted);
        modelAndView.addObject("confirmedOrders", confirmed);
        modelAndView.addObject("deliveredOrders", delivered);
        return modelAndView;
    }

    @GetMapping("/{id}")
    public ModelAndView getOrderDetails(@PathVariable("id") UUID orderId,
                                        @ModelAttribute("message") String message,
                                        @ModelAttribute("error") String error) {
        Order order = orderService.getOrderById(orderId);

        ModelAndView modelAndView = new ModelAndView("admin-order-details");
        modelAndView.addObject("order", order);
        return modelAndView;
    }

    @PatchMapping("/{id}")
    public String updateOrderStatus(@PathVariable("id") UUID orderId,
                                    @RequestParam("status") OrderStatus status) {
        orderService.changeAdminOrderStatus(orderId, status);
        return "redirect:/admin/orders/" + orderId;
    }
}
