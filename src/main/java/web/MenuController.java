package web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import product.model.ProductCategory;
import product.service.ProductService;
import security.UserData;

@Controller
public class MenuController {
    private final ProductService productService;

    public MenuController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/menu")
    public ModelAndView getMenuPage(@AuthenticationPrincipal UserData userData) {
        ModelAndView modelAndView = new ModelAndView("menu");

        modelAndView.addObject("starters", productService.getByCategory(ProductCategory.STARTER));
        modelAndView.addObject("mains", productService.getByCategory(ProductCategory.MAIN));
        modelAndView.addObject("desserts", productService.getByCategory(ProductCategory.DESSERT));
        modelAndView.addObject("drinks", productService.getByCategory(ProductCategory.DRINK));

        modelAndView.addObject("isAuthenticated", userData != null);

        return modelAndView;
    }
}
