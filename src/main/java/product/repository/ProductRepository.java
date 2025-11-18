package product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import product.model.Product;
import product.model.ProductCategory;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByActiveTrueOrderByNameAsc();

    List<Product> findAllByActiveTrueAndCategoryOrderByNameAsc(ProductCategory category);
}
