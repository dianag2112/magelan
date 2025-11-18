package web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import product.model.Product;
import product.model.ProductCategory;
import product.service.ProductService;
import web.dto.ProductForm;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ModelAndView listProducts() {
        List<Product> products = productService.getAll();
        ModelAndView modelAndView = new ModelAndView("admin-products");
        modelAndView.addObject("products", products);
        modelAndView.addObject("categories", Arrays.asList(ProductCategory.values()));
        return modelAndView;
    }

    @GetMapping("/form")
    public ModelAndView showCreateForm() {
        ProductForm form = ProductForm.builder()
                .active(true)
                .build();

        ModelAndView modelAndView = new ModelAndView("admin-product-form");
        modelAndView.addObject("productForm", form);
        modelAndView.addObject("categories", ProductCategory.values());
        modelAndView.addObject("mode", "create");
        return modelAndView;
    }

    @PostMapping
    public ModelAndView createProduct(@Valid @ModelAttribute("productForm") ProductForm form,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("admin-product-form");
            modelAndView.addObject("categories", ProductCategory.values());
            modelAndView.addObject("mode", "create");
            return modelAndView;
        }

        productService.create(form);
        redirectAttributes.addFlashAttribute("message", "Product created successfully.");
        return new ModelAndView("redirect:/admin/products");
    }

    @GetMapping("/{id}")
    public ModelAndView showEditForm(@PathVariable("id") UUID id) {
        Product product = productService.getById(id);
        ProductForm form = productService.toForm(product);

        ModelAndView modelAndView = new ModelAndView("admin-product-form");
        modelAndView.addObject("productForm", form);
        modelAndView.addObject("categories", ProductCategory.values());
        modelAndView.addObject("mode", "edit");
        return modelAndView;
    }

    @PutMapping("/{id}")
    public ModelAndView updateProduct(@PathVariable("id") UUID id,
                                      @Valid @ModelAttribute("productForm") ProductForm form,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("admin-product-form");
            modelAndView.addObject("categories", ProductCategory.values());
            modelAndView.addObject("mode", "edit");
            return modelAndView;
        }

        productService.update(id, form);
        redirectAttributes.addFlashAttribute("message", "Product updated successfully.");
        return new ModelAndView("redirect:/admin/products");
    }

    @DeleteMapping("/{id}")
    public ModelAndView deleteProduct(@PathVariable("id") UUID id,
                                      RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Product deleted.");
        return new ModelAndView("redirect:/admin/products");
    }
}
