package backend.controller;

import backend.dto.AuthDtos.MessageResponse;
import backend.dto.DemandDtos.SignalRequest;
import backend.security.CurrentUser;
import backend.service.DemandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demand")
public class DemandController {

    private final DemandService demand;

    public DemandController(DemandService demand) {
        this.demand = demand;
    }

    @PostMapping("/signals")
    public ResponseEntity<MessageResponse> signal(@Valid @RequestBody SignalRequest req) {
        demand.recordSignal(CurrentUser.require().getUserId(), req);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new MessageResponse("Thanks - nearby vendors can now see there's interest."));
    }
}
