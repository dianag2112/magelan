package bg.softuni.magelan.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import bg.softuni.magelan.product.model.Product;
import bg.softuni.magelan.product.model.ProductCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByActiveTrueOrderByNameAsc();

    List<Product> findAllByActiveTrueAndCategoryOrderByNameAsc(ProductCategory category);

    List<Product> findAllByCategoryOrderByNameAsc(ProductCategory category);

    Optional<Product> findByIdAndActiveTrue(UUID id);
}
