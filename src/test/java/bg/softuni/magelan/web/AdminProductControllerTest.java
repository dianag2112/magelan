package bg.softuni.magelan.web;

import bg.softuni.magelan.product.model.Product;
import bg.softuni.magelan.product.model.ProductCategory;
import bg.softuni.magelan.product.service.ProductService;
import bg.softuni.magelan.web.dto.ProductForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private AdminProductController adminProductController;

    @Mock
    private RedirectAttributes redirectAttributes;

    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = Product.builder()
                .id(productId)
                .name("Magelan Burger")
                .description("Tasty burger")
                .price(new BigDecimal("14.90"))
                .category(ProductCategory.MAIN)
                .active(true)
                .build();
    }

    @Test
    void listProducts_shouldReturnViewWithProductsAndCategories() {
        List<Product> products = List.of(product);
        when(productService.getAll()).thenReturn(products);

        ModelAndView mav = adminProductController.listProducts();

        assertThat(mav.getViewName()).isEqualTo("admin-products");
        assertThat(mav.getModel()).containsKeys("products", "categories");

        assertThat(mav.getModel().get("products")).isEqualTo(products);
        Object categoriesObj = mav.getModel().get("categories");
        assertThat(categoriesObj).isInstanceOf(List.class);
        List<ProductCategory> categories = (List<ProductCategory>) categoriesObj;
        assertThat(categories).containsExactlyInAnyOrder(ProductCategory.values());
        verify(productService).getAll();
    }

    @Test
    void showCreateForm_shouldReturnEmptyFormWithDefaults() {
        ModelAndView mav = adminProductController.showCreateForm();

        assertThat(mav.getViewName()).isEqualTo("admin-product-form");
        assertThat(mav.getModel()).containsKeys("productForm", "categories", "mode");
        assertThat(mav.getModel().get("mode")).isEqualTo("create");

        ProductForm form = (ProductForm) mav.getModel().get("productForm");
        assertThat(form.isActive()).isTrue();

        Object categoriesObj = mav.getModel().get("categories");
        assertThat(categoriesObj).isInstanceOf(ProductCategory[].class);
        ProductCategory[] categories = (ProductCategory[]) categoriesObj;
        assertThat(categories).containsExactlyInAnyOrder(ProductCategory.values());
    }

    @Test
    void createProduct_shouldReturnFormWhenValidationFails() {
        ProductForm form = new ProductForm();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        ModelAndView mav = adminProductController.createProduct(form, bindingResult, redirectAttributes);

        assertThat(mav.getViewName()).isEqualTo("admin-product-form");
        assertThat(mav.getModel()).containsEntry("mode", "create");
        assertThat(mav.getModel()).containsKey("categories");

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_shouldCreateAndRedirect_whenValid() {
        ProductForm form = ProductForm.builder()
                .name("New product")
                .price(new BigDecimal("5.50"))
                .category(ProductCategory.STARTER)
                .active(true)
                .build();

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = adminProductController.createProduct(form, bindingResult, redirectAttributes);

        assertThat(mav.getViewName()).isEqualTo("redirect:/admin/products");
        verify(productService).create(form);
        verify(redirectAttributes).addFlashAttribute("message", "Product created successfully.");
    }

    @Test
    void showEditForm_shouldLoadProductAndForm() {
        ProductForm form = ProductForm.builder()
                .id(productId.toString())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .active(product.isActive())
                .build();

        when(productService.getById(productId)).thenReturn(product);
        when(productService.toForm(product)).thenReturn(form);

        ModelAndView mav = adminProductController.showEditForm(productId);

        assertThat(mav.getViewName()).isEqualTo("admin-product-form");
        assertThat(mav.getModel()).containsEntry("mode", "edit");
        assertThat(mav.getModel()).containsKeys("productForm", "categories");

        ProductForm modelForm = (ProductForm) mav.getModel().get("productForm");
        assertThat(modelForm.getId()).isEqualTo(productId.toString());
        assertThat(modelForm.getName()).isEqualTo(product.getName());

        verify(productService).getById(productId);
        verify(productService).toForm(product);
    }

    @Test
    void updateProduct_shouldReturnFormWhenValidationFails() {
        ProductForm form = new ProductForm();
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        ModelAndView mav = adminProductController.updateProduct(productId, form, bindingResult, redirectAttributes);

        assertThat(mav.getViewName()).isEqualTo("admin-product-form");
        assertThat(mav.getModel()).containsEntry("mode", "edit");
        assertThat(mav.getModel()).containsKey("categories");

        verifyNoInteractions(productService);
    }

    @Test
    void updateProduct_shouldUpdateAndRedirect_whenValid() {
        ProductForm form = ProductForm.builder()
                .name("Updated name")
                .price(new BigDecimal("11.00"))
                .category(ProductCategory.MAIN)
                .active(false)
                .build();

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        ModelAndView mav = adminProductController.updateProduct(productId, form, bindingResult, redirectAttributes);

        assertThat(mav.getViewName()).isEqualTo("redirect:/admin/products");
        verify(productService).update(productId, form);
        verify(redirectAttributes).addFlashAttribute("message", "Product updated successfully.");
    }

    @Test
    void deleteProduct_shouldDeleteAndRedirect() {
        ModelAndView mav = adminProductController.deleteProduct(productId, redirectAttributes);

        assertThat(mav.getViewName()).isEqualTo("redirect:/admin/products");
        verify(productService).delete(productId);
        verify(redirectAttributes).addFlashAttribute("message", "Product deleted.");
    }
}
