package backend.controller;

import backend.dto.VendorDtos.ReviewDto;
import backend.dto.VendorDtos.VendorDetail;
import backend.dto.VendorDtos.VendorSummary;
import backend.model.Category;
import backend.service.VendorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public browsing: no sign-in needed to see who is nearby. */
@RestController
@RequestMapping("/api/vendors")
public class VendorPublicController {

    private final VendorService vendors;

    public VendorPublicController(VendorService vendors) {
        this.vendors = vendors;
    }

    @GetMapping("/nearby")
    public List<VendorSummary> nearby(@RequestParam("lat") double lat,
                                      @RequestParam("lng") double lng,
                                      @RequestParam(value = "radiusKm", required = false) Double radiusKm,
                                      @RequestParam(value = "category", required = false) Category category,
                                      @RequestParam(value = "q", required = false) String q,
                                      @RequestParam(value = "onlineOnly", defaultValue = "false") boolean onlineOnly) {
        return vendors.nearby(lat, lng, radiusKm, category, q, onlineOnly);
    }

    @GetMapping("/{id}")
    public VendorDetail detail(@PathVariable("id") Long id) {
        return vendors.detail(id);
    }

    @GetMapping("/{id}/reviews")
    public List<ReviewDto> reviews(@PathVariable("id") Long id) {
        return vendors.reviewsFor(id);
    }
}
