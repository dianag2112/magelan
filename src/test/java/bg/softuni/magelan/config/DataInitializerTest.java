package bg.softuni.magelan.config;

import bg.softuni.magelan.product.model.Product;
import bg.softuni.magelan.product.model.ProductCategory;
import bg.softuni.magelan.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_shouldSeedProducts_whenRepositoryEmpty() {

        when(productRepository.count()).thenReturn(0L);

        dataInitializer.run();

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).save(captor.capture());

        List<Product> savedProducts = captor.getAllValues();
        assertThat(savedProducts).isNotEmpty();

        assertThat(savedProducts)
                .anyMatch(p -> p.getName().equals("Captain's Garlic Bread")
                        && p.getCategory() == ProductCategory.STARTER);

        assertThat(savedProducts)
                .anyMatch(p -> p.getName().equals("Magelan Burger")
                        && p.getCategory() == ProductCategory.MAIN);

        assertThat(savedProducts)
                .allMatch(Product::isActive)
                .allMatch(p -> p.getPrice().compareTo(BigDecimal.ZERO) > 0)
                .allMatch(p -> p.getCreatedOn() != null && p.getUpdatedOn() != null);
    }

    @Test
    void run_shouldDoNothing_whenProductsAlreadyExist() {

        when(productRepository.count()).thenReturn(5L);

        dataInitializer.run();

        verify(productRepository, never()).save(any());
    }
}
