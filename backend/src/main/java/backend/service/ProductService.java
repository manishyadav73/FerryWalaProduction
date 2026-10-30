package backend.service;

import backend.dto.ProductDtos.ProductDto;
import backend.dto.ProductDtos.ProductRequest;
import backend.error.ApiException;
import backend.model.Product;
import backend.model.Vendor;
import backend.repository.ProductRepository;
import backend.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class ProductService {

    private static final int MAX_PRODUCTS_PER_VENDOR = 200;

    private final ProductRepository products;
    private final VendorRepository vendors;

    public ProductService(ProductRepository products, VendorRepository vendors) {
        this.products = products;
        this.vendors = vendors;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listOwn(Long userId) {
        Vendor vendor = vendorOf(userId);
        return products.findByVendorIdOrderByNameAsc(vendor.getId()).stream().map(ProductService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listPublic(Long vendorId) {
        return products.findByVendorIdAndAvailableTrueOrderByNameAsc(vendorId).stream()
                .map(ProductService::toDto).toList();
    }

    @Transactional
    public ProductDto create(Long userId, ProductRequest req) {
        Vendor vendor = vendorOf(userId);
        if (products.countByVendorId(vendor.getId()) >= MAX_PRODUCTS_PER_VENDOR) {
            throw ApiException.conflict("PRODUCT_LIMIT", "You've reached the limit of " + MAX_PRODUCTS_PER_VENDOR
                    + " products. Remove one to add another.");
        }
        Product p = new Product();
        p.setVendor(vendor);
        apply(p, req);
        return toDto(products.save(p));
    }

    @Transactional
    public ProductDto update(Long userId, Long productId, ProductRequest req) {
        Vendor vendor = vendorOf(userId);
        Product p = products.findByIdAndVendorId(productId, vendor.getId())
                .orElseThrow(() -> ApiException.notFound("Product not found."));
        apply(p, req);
        return toDto(p);
    }

    @Transactional
    public ProductDto setAvailability(Long userId, Long productId, boolean available) {
        Vendor vendor = vendorOf(userId);
        Product p = products.findByIdAndVendorId(productId, vendor.getId())
                .orElseThrow(() -> ApiException.notFound("Product not found."));
        p.setAvailable(available);
        p.setUpdatedAt(Instant.now());
        return toDto(p);
    }

    @Transactional
    public void delete(Long userId, Long productId) {
        Vendor vendor = vendorOf(userId);
        Product p = products.findByIdAndVendorId(productId, vendor.getId())
                .orElseThrow(() -> ApiException.notFound("Product not found."));
        products.delete(p);
    }

    private Vendor vendorOf(Long userId) {
        return vendors.findByUserId(userId)
                .orElseThrow(() -> ApiException.forbidden("No vendor profile found for this account."));
    }

    private static void apply(Product p, ProductRequest req) {
        p.setName(req.name().replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim());
        p.setCategory(req.category());
        p.setPrice(req.price().setScale(2, RoundingMode.HALF_UP));
        p.setUnit(req.unit().replaceAll("\\p{Cntrl}", " ").trim());
        p.setStock(req.stock());
        p.setAvailable(req.available() == null || req.available());
        String img = req.imageUrl() == null ? "" : req.imageUrl().trim();
        p.setImageUrl(img.isEmpty() ? null : img);
        p.setUpdatedAt(Instant.now());
        if (p.getName().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Product name is required.");
        }
        if (p.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Price must be greater than 0.");
        }
    }

    public static ProductDto toDto(Product p) {
        Vendor v = p.getVendor();
        return new ProductDto(p.getId(), v.getId(), v.getShopName(), p.getName(), p.getCategory(), p.getPrice(),
                p.getUnit(), p.getStock(), p.isAvailable(), p.getImageUrl());
    }
}
