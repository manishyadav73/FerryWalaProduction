package backend.controller;

import backend.dto.VendorDtos.LocationRequest;
import backend.dto.VendorDtos.ReviewDto;
import backend.dto.VendorDtos.StatusRequest;
import backend.dto.VendorDtos.UpdateVendorRequest;
import backend.dto.VendorDtos.VendorSummary;
import backend.dto.DemandDtos.OpportunityResponse;
import backend.security.CurrentUser;
import backend.service.DemandService;
import backend.service.OrderService;
import backend.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Everything a signed-in vendor does to their own shop. Role enforced in SecurityConfig. */
@RestController
@RequestMapping("/api/vendor")
public class VendorSelfController {

    private final VendorService vendors;
    private final DemandService demand;
    private final OrderService orders;

    public VendorSelfController(VendorService vendors, DemandService demand, OrderService orders) {
        this.vendors = vendors;
        this.demand = demand;
        this.orders = orders;
    }

    @GetMapping("/me")
    public VendorSummary me() {
        return vendors.me(CurrentUser.require().getUserId());
    }

    @PutMapping("/me")
    public VendorSummary update(@Valid @RequestBody UpdateVendorRequest req) {
        return vendors.updateProfile(CurrentUser.require().getUserId(), req);
    }

    @PutMapping("/me/status")
    public VendorSummary status(@Valid @RequestBody StatusRequest req) {
        return vendors.setOnline(CurrentUser.require().getUserId(), req.online());
    }

    @PutMapping("/me/location")
    public VendorSummary location(@Valid @RequestBody LocationRequest req) {
        return vendors.updateLocation(CurrentUser.require().getUserId(), req);
    }

    @PostMapping(value = "/me/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VendorSummary uploadImage(@RequestParam("file") MultipartFile file) {
        return vendors.replaceImage(CurrentUser.require().getUserId(), file);
    }

    @DeleteMapping("/me/image")
    public VendorSummary deleteImage() {
        return vendors.removeImage(CurrentUser.require().getUserId());
    }

    @GetMapping("/reviews")
    public List<ReviewDto> reviews() {
        Long vendorId = vendors.vendorOf(CurrentUser.require().getUserId()).getId();
        return vendors.reviewsFor(vendorId);
    }

    @GetMapping("/stats")
    public OrderService.VendorStats stats() {
        return orders.vendorStats(CurrentUser.require().getUserId());
    }

    @GetMapping("/opportunities")
    public OpportunityResponse opportunities(@RequestParam(value = "lat", required = false) Double lat,
                                             @RequestParam(value = "lng", required = false) Double lng,
                                             @RequestParam(value = "radiusKm", required = false) Double radiusKm) {
        return demand.opportunities(CurrentUser.require().getUserId(), lat, lng, radiusKm);
    }
}
