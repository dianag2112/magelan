package web;

import order.model.Order;
import order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import security.UserData;
import user.model.User;
import user.service.UserService;

import java.util.UUID;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final UserService userService;
    private final OrderService orderService;

    @Autowired
    public OrderController(UserService userService,
                           OrderService orderService) {
        this.userService = userService;
        this.orderService = orderService;
    }

    @GetMapping
    public ModelAndView getOrdersPage(@AuthenticationPrincipal UserData userData) {
        User user = userService.getById(userData.getUserId());

        Order order = orderService.getOrCreatePendingOrderForCustomer(user);

        ModelAndView modelAndView = new ModelAndView("orders");
        modelAndView.addObject("user", user);
        modelAndView.addObject("orderItems", order.getItems());
        modelAndView.addObject("products", orderService.getAvailableProducts());
        modelAndView.addObject("totalAmount", orderService.calculateTotal(order));
        modelAndView.addObject("pastOrders", orderService.getPastOrders(user.getId()));
        modelAndView.addObject("isAuthenticated", userData != null);
        return modelAndView;
    }

    @PostMapping("/items")
    public String addProductToOrder(@AuthenticationPrincipal UserData userData,
                                    @RequestParam("productId") UUID productId,
                                    @RequestParam("quantity") int quantity) {

        if (userData == null) {
            return "redirect:/login";
        }

        User user = userService.getById(userData.getUserId());
        orderService.addProductToCustomerOrder(user, productId, quantity);

        return "redirect:/orders";
    }

    @DeleteMapping("/items/{itemId}")
    public String removeItem(@AuthenticationPrincipal UserData userData,
                             @PathVariable("itemId") UUID itemId) {

        UUID userId = userData.getUserId();
        orderService.removeItemFromOrder(userId, itemId);

        return "redirect:/orders";
    }

    @PostMapping
    public String submitOrder(@AuthenticationPrincipal UserData userData,
                              @RequestParam("fullName") String fullName,
                              @RequestParam("phone") String phone,
                              @RequestParam("address") String address,
                              @RequestParam(value = "notes", required = false) String notes) {

        UUID userId = userData.getUserId();
        orderService.confirmPendingOrder(userId, fullName, phone, address, notes);

        return "redirect:/orders";
    }
}
