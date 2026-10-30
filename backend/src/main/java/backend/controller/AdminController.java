package backend.controller;

import backend.demand.DemandEngine.Opportunity;
import backend.dto.AdminDtos.AdminUserDto;
import backend.dto.AdminDtos.AdminVendorDto;
import backend.dto.AdminDtos.EnabledRequest;
import backend.dto.AdminDtos.PageDto;
import backend.dto.AdminDtos.Stats;
import backend.dto.AdminDtos.SuspiciousFlag;
import backend.dto.AdminDtos.VerifiedRequest;
import backend.dto.OrderDtos.OrderDto;
import backend.dto.ProductDtos.ProductDto;
import backend.security.CurrentUser;
import backend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** ADMIN role is enforced for /api/admin/** in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/stats")
    public Stats stats() {
        return admin.stats();
    }

    @GetMapping("/users")
    public PageDto<AdminUserDto> users(@RequestParam(value = "q", required = false) String q,
                                       @RequestParam(value = "page", defaultValue = "0") int page,
                                       @RequestParam(value = "size", defaultValue = "25") int size) {
        return admin.users(q, page, size);
    }

    @PutMapping("/users/{id}/enabled")
    public AdminUserDto setEnabled(@PathVariable("id") Long id, @Valid @RequestBody EnabledRequest req) {
        return admin.setUserEnabled(CurrentUser.require().getUserId(), id, req.enabled());
    }

    @GetMapping("/vendors")
    public PageDto<AdminVendorDto> vendors(@RequestParam(value = "page", defaultValue = "0") int page,
                                           @RequestParam(value = "size", defaultValue = "25") int size) {
        return admin.vendors(page, size);
    }

    @PutMapping("/vendors/{id}/verified")
    public AdminVendorDto setVerified(@PathVariable("id") Long id, @Valid @RequestBody VerifiedRequest req) {
        return admin.setVendorVerified(id, req.verified());
    }

    @GetMapping("/products")
    public PageDto<ProductDto> products(@RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "25") int size) {
        return admin.products(page, size);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) {
        admin.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public List<OrderDto> orders(@RequestParam(value = "status", required = false) String status) {
        return admin.orders(status);
    }

    @GetMapping("/demand/areas")
    public List<Opportunity> demandAreas() {
        return admin.demandAreas();
    }

    @GetMapping("/suspicious")
    public List<SuspiciousFlag> suspicious() {
        return admin.suspicious();
    }
}
