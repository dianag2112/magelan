package bg.softuni.magelan.product.service;

import bg.softuni.magelan.product.model.Product;
import bg.softuni.magelan.product.model.ProductCategory;
import bg.softuni.magelan.product.repository.ProductRepository;
import bg.softuni.magelan.web.dto.ProductForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getByCategory_shouldDelegateToRepository() {
        ProductCategory category = ProductCategory.MAIN;
        List<Product> products = List.of(new Product());
        when(productRepository.findAllByCategoryOrderByNameAsc(category))
                .thenReturn(products);

        List<Product> result = productService.getByCategory(category);

        assertThat(result).isSameAs(products);
        verify(productRepository).findAllByCategoryOrderByNameAsc(category);
    }

    @Test
    void getAll_shouldDelegateToRepository() {
        List<Product> products = List.of(new Product(), new Product());
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getAll();

        assertThat(result).isSameAs(products);
        verify(productRepository).findAll();
    }

    @Test
    void getById_shouldReturnProduct_whenFound() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder().id(id).name("Test").build();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        Product result = productService.getById(id);

        assertThat(result).isSameAs(product);
        verify(productRepository).findById(id);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Product with ID [%s] not found.".formatted(id));

        verify(productRepository).findById(id);
    }

    @Test
    void create_shouldMapFormAndSaveProduct() {
        ProductForm form = ProductForm.builder()
                .name("Magelan Burger")
                .description("Tasty burger")
                .price(new BigDecimal("14.90"))
                .category(ProductCategory.MAIN)
                .active(true)
                .build();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        Product saved = Product.builder()
                .id(UUID.randomUUID())
                .name(form.getName())
                .description(form.getDescription())
                .price(form.getPrice())
                .category(form.getCategory())
                .active(form.isActive())
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        Product result = productService.create(form);

        verify(productRepository).save(productCaptor.capture());
        Product toSave = productCaptor.getValue();

        assertThat(toSave.getName()).isEqualTo(form.getName());
        assertThat(toSave.getDescription()).isEqualTo(form.getDescription());
        assertThat(toSave.getPrice()).isEqualByComparingTo(form.getPrice());
        assertThat(toSave.getCategory()).isEqualTo(form.getCategory());
        assertThat(toSave.isActive()).isEqualTo(form.isActive());
        assertThat(toSave.getCreatedOn()).isNotNull();
        assertThat(toSave.getUpdatedOn()).isNotNull();

        assertThat(result).isSameAs(saved);
    }

    @Test
    void update_shouldUpdateExistingProductAndSave() {
        UUID id = UUID.randomUUID();

        Product existing = Product.builder()
                .id(id)
                .name("Old name")
                .description("Old desc")
                .price(new BigDecimal("10.00"))
                .category(ProductCategory.STARTER)
                .active(false)
                .createdOn(LocalDateTime.now().minusDays(1))
                .updatedOn(LocalDateTime.now().minusDays(1))
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductForm form = ProductForm.builder()
                .name("New name")
                .description("New desc")
                .price(new BigDecimal("12.50"))
                .category(ProductCategory.MAIN)
                .active(true)
                .build();

        Product result = productService.update(id, form);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("New name");
        assertThat(result.getDescription()).isEqualTo("New desc");
        assertThat(result.getPrice()).isEqualByComparingTo("12.50");
        assertThat(result.getCategory()).isEqualTo(ProductCategory.MAIN);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getUpdatedOn()).isNotNull();
        assertThat(result.getUpdatedOn()).isAfter(existing.getCreatedOn());

        verify(productRepository).findById(id);
        verify(productRepository).save(existing);
    }

    @Test
    void delete_shouldDelegateToRepository() {
        UUID id = UUID.randomUUID();

        productService.delete(id);

        verify(productRepository).deleteById(id);
    }

    @Test
    void toForm_shouldMapAllFields() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
                .id(id)
                .name("Black Pearl Bites")
                .description("Crispy bites")
                .price(new BigDecimal("8.90"))
                .category(ProductCategory.STARTER)
                .active(true)
                .build();

        ProductForm form = productService.toForm(product);

        assertThat(form.getId()).isEqualTo(id.toString());
        assertThat(form.getName()).isEqualTo(product.getName());
        assertThat(form.getDescription()).isEqualTo(product.getDescription());
        assertThat(form.getPrice()).isEqualByComparingTo(product.getPrice());
        assertThat(form.getCategory()).isEqualTo(product.getCategory());
        assertThat(form.isActive()).isTrue();
    }
}
