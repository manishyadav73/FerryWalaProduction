package backend.controller;

import backend.dto.OrderDtos.OrderDto;
import backend.dto.OrderDtos.PlaceOrderRequest;
import backend.dto.OrderDtos.ReviewRequest;
import backend.dto.AuthDtos.MessageResponse;
import backend.security.CurrentUser;
import backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    public ResponseEntity<OrderDto> place(@Valid @RequestBody PlaceOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orders.place(CurrentUser.require().getUserId(), req));
    }

    @GetMapping("/mine")
    public List<OrderDto> mine() {
        return orders.mine(CurrentUser.require().getUserId());
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable("id") Long id) {
        return orders.get(CurrentUser.require(), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderDto cancel(@PathVariable("id") Long id) {
        return orders.cancelByCustomer(CurrentUser.require().getUserId(), id);
    }

    @PostMapping("/{id}/review")
    public MessageResponse review(@PathVariable("id") Long id, @Valid @RequestBody ReviewRequest req) {
        orders.review(CurrentUser.require().getUserId(), id, req);
        return new MessageResponse("Thanks for your feedback!");
    }
}
