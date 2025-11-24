package bg.softuni.magelan.product.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import bg.softuni.magelan.product.model.Product;
import bg.softuni.magelan.product.model.ProductCategory;
import bg.softuni.magelan.product.repository.ProductRepository;
import bg.softuni.magelan.web.dto.ProductForm;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAvailableProducts() {
        log.debug("Fetching all active products.");
        return productRepository.findAllByActiveTrueOrderByNameAsc();
    }

    public List<Product> getByCategory(ProductCategory category) {
        log.debug("Fetching products in category {}", category);
        return productRepository.findAllByCategoryOrderByNameAsc(category);
    }

    public List<Product> getAll() {
        log.debug("Fetching all products (including inactive).");
        return productRepository.findAll();
    }

    public Product getById(UUID id) {
        log.debug("Fetching product with ID {}", id);

        return productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product with ID {} not found.", id);
                    return new RuntimeException("Product with ID [%s] not found.".formatted(id));
                });
    }

    public Product create(ProductForm form) {
        LocalDateTime now = LocalDateTime.now();

        log.info("Creating new product '{}', price {}, category {}",
                form.getName(), form.getPrice(), form.getCategory());

        Product product = Product.builder()
                .name(form.getName())
                .description(form.getDescription())
                .price(form.getPrice())
                .category(form.getCategory())
                .active(form.isActive())
                .createdOn(now)
                .updatedOn(now)
                .build();

        Product saved = productRepository.save(product);

        log.info("Product {} created successfully.", saved.getId());
        return saved;
    }

    public Product update(UUID id, ProductForm form) {
        log.info("Updating product {}", id);

        Product product = getById(id);

        product.setName(form.getName());
        product.setDescription(form.getDescription());
        product.setPrice(form.getPrice());
        product.setCategory(form.getCategory());
        product.setActive(form.isActive());
        product.setUpdatedOn(LocalDateTime.now());

        Product saved = productRepository.save(product);

        log.info("Product {} updated successfully.", id);
        return saved;
    }

    public void delete(UUID id) {
            log.info("Hard deleting product {}", id);
            productRepository.deleteById(id);
        }

    public ProductForm toForm(Product product) {
        log.debug("Converting product {} to ProductForm DTO", product.getId());

        return ProductForm.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .category(product.getCategory())
                .active(product.isActive())
                .build();
    }
}
