package config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import product.model.Product;
import product.model.ProductCategory;
import product.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        create("Captain's Garlic Bread",
                "Toasted rustic bread with garlic butter, herbs & a hint of sea salt.",
                "5.90", ProductCategory.STARTER, now);

        create("Harbor Soup",
                "Creamy roasted vegetables with smoked paprika & crispy croutons.",
                "7.40", ProductCategory.STARTER, now);

        create("Black Pearl Bites",
                "Crispy breaded bites with spicy dip, perfect for sharing your plunder.",
                "8.90", ProductCategory.STARTER, now);

        create("Magelan Burger",
                "Juicy beef patty, cheddar, caramelized onions & Magelan sauce in a toasted bun.",
                "14.90", ProductCategory.MAIN, now);

        create("Crimson Chicken Steak",
                "Marinated chicken fillet, grilled & served with smoky butter and fries.",
                "13.50", ProductCategory.MAIN, now);

        create("Veggie Corsair Bowl",
                "Roasted veggies, herbed rice, chickpeas & tangy house dressing.",
                "12.40", ProductCategory.MAIN, now);

        create("Midnight Ribs",
                "Slow-cooked pork ribs glazed with dark BBQ sauce, served with wedges.",
                "18.90", ProductCategory.MAIN, now);

        create("Black Forest Treasure",
                "Rich chocolate cake with cherry filling & dark chocolate drizzle.",
                "6.90", ProductCategory.DESSERT, now);

        create("Sea Mist Cheesecake",
                "Creamy baked cheesecake with berry coulis.",
                "6.40", ProductCategory.DESSERT, now);

        create("Craft Lemonade",
                "Classic / Raspberry / Elderflower.",
                "3.90", ProductCategory.DRINK, now);

        create("Espresso",
                "Short, strong, unforgiving.",
                "2.40", ProductCategory.DRINK, now);

        create("Soft Drinks",
                "Selection of bottled soft drinks.",
                "2.90", ProductCategory.DRINK, now);
    }

    private void create(String name, String desc, String price,
                        ProductCategory category, LocalDateTime now) {
        Product p = Product.builder()
                .name(name)
                .description(desc)
                .price(new BigDecimal(price))
                .active(true)
                .category(category)
                .createdOn(now)
                .updatedOn(now)
                .build();

        productRepository.save(p);
    }
}
