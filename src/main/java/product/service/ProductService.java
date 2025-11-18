package product.service;

import org.springframework.stereotype.Service;
import product.model.Product;
import product.model.ProductCategory;
import product.repository.ProductRepository;
import web.dto.ProductForm;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAvailableProducts() {
        return productRepository.findAllByActiveTrueOrderByNameAsc();
    }

    public List<Product> getByCategory(ProductCategory category) {
        return productRepository.findAllByActiveTrueAndCategoryOrderByNameAsc(category);
    }

    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public Product getById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product with ID [%s] not found.".formatted(id)));
    }

    public Product create(ProductForm form) {
        LocalDateTime now = LocalDateTime.now();

        Product product = Product.builder()
                .name(form.getName())
                .description(form.getDescription())
                .price(form.getPrice())
                .category(form.getCategory())
                .active(form.isActive())
                .createdOn(now)
                .updatedOn(now)
                .build();

        return productRepository.save(product);
    }

    public Product update(UUID id, ProductForm form) {
        Product product = getById(id);

        product.setName(form.getName());
        product.setDescription(form.getDescription());
        product.setPrice(form.getPrice());
        product.setCategory(form.getCategory());
        product.setActive(form.isActive());
        product.setUpdatedOn(LocalDateTime.now());

        return productRepository.save(product);
    }

    public void delete(UUID id) {
        Product product = getById(id);
        product.setActive(false);
        product.setUpdatedOn(LocalDateTime.now());
        productRepository.save(product);
    }

    public ProductForm toForm(Product product) {
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
