package backend.controller;

import backend.dto.ProductDtos.AvailabilityRequest;
import backend.dto.ProductDtos.ProductDto;
import backend.dto.ProductDtos.ProductRequest;
import backend.security.CurrentUser;
import backend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/products")
public class VendorProductController {

    private final ProductService products;

    public VendorProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    public List<ProductDto> list() {
        return products.listOwn(CurrentUser.require().getUserId());
    }

    @PostMapping
    public ResponseEntity<ProductDto> create(@Valid @RequestBody ProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(products.create(CurrentUser.require().getUserId(), req));
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable("id") Long id, @Valid @RequestBody ProductRequest req) {
        return products.update(CurrentUser.require().getUserId(), id, req);
    }

    @PatchMapping("/{id}/availability")
    public ProductDto availability(@PathVariable("id") Long id, @Valid @RequestBody AvailabilityRequest req) {
        return products.setAvailability(CurrentUser.require().getUserId(), id, req.available());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        products.delete(CurrentUser.require().getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
