package backend.controller;

import backend.dto.OrderDtos.OrderDto;
import backend.dto.OrderDtos.UpdateOrderStatusRequest;
import backend.security.CurrentUser;
import backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/orders")
public class VendorOrderController {

    private final OrderService orders;

    public VendorOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public List<OrderDto> list(@RequestParam(value = "status", required = false) String status) {
        return orders.forVendor(CurrentUser.require().getUserId(), status);
    }

    @PutMapping("/{id}/status")
    public OrderDto updateStatus(@PathVariable("id") Long id, @Valid @RequestBody UpdateOrderStatusRequest req) {
        return orders.transition(CurrentUser.require().getUserId(), id, req.status());
    }
}
