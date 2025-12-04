package bg.softuni.magelan.web;

import bg.softuni.magelan.product.model.Product;
import bg.softuni.magelan.product.model.ProductCategory;
import bg.softuni.magelan.product.service.ProductService;
import bg.softuni.magelan.security.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuControllerTest {

    @Mock
    private ProductService productService;

    private MenuController menuController;

    @BeforeEach
    void setUp() {
        menuController = new MenuController(productService);
    }

    private Product product(String name, ProductCategory category) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name(name)
                .category(category)
                .price(BigDecimal.TEN)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    @Test
    void getMenuPage_shouldPopulateModelAndSetIsAuthenticatedFalse_whenNoUser() {
        List<Product> starters = List.of(product("Starter", ProductCategory.STARTER));
        List<Product> mains = List.of(product("Main", ProductCategory.MAIN));
        List<Product> desserts = List.of(product("Dessert", ProductCategory.DESSERT));
        List<Product> drinks = List.of(product("Drink", ProductCategory.DRINK));

        when(productService.getByCategory(ProductCategory.STARTER)).thenReturn(starters);
        when(productService.getByCategory(ProductCategory.MAIN)).thenReturn(mains);
        when(productService.getByCategory(ProductCategory.DESSERT)).thenReturn(desserts);
        when(productService.getByCategory(ProductCategory.DRINK)).thenReturn(drinks);

        ModelAndView mav = menuController.getMenuPage(null);

        assertEquals("menu", mav.getViewName());
        assertSame(starters, mav.getModel().get("starters"));
        assertSame(mains, mav.getModel().get("mains"));
        assertSame(desserts, mav.getModel().get("desserts"));
        assertSame(drinks, mav.getModel().get("drinks"));
        assertEquals(false, mav.getModel().get("isAuthenticated"));
    }

    @Test
    void getMenuPage_shouldSetIsAuthenticatedTrue_whenUserPresent() {
        UserData userData = new UserData(
                UUID.randomUUID(),
                "pirate",
                "pwd",
                null,
                true
        );

        when(productService.getByCategory(ProductCategory.STARTER)).thenReturn(List.of());
        when(productService.getByCategory(ProductCategory.MAIN)).thenReturn(List.of());
        when(productService.getByCategory(ProductCategory.DESSERT)).thenReturn(List.of());
        when(productService.getByCategory(ProductCategory.DRINK)).thenReturn(List.of());

        ModelAndView mav = menuController.getMenuPage(userData);

        assertEquals("menu", mav.getViewName());
        assertEquals(true, mav.getModel().get("isAuthenticated"));
    }
}
