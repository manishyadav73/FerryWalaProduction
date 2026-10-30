package backend.controller;

import backend.dto.ConciergeDtos.ChatRequest;
import backend.dto.ConciergeDtos.ChatResponse;
import backend.dto.ConciergeDtos.ConciergeStatus;
import backend.dto.ConciergeDtos.HistoryItem;
import backend.security.CurrentUser;
import backend.service.ConciergeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Any signed-in user may call these; ConciergeService only serves customers and vendors. */
@RestController
@RequestMapping("/api/concierge")
public class ConciergeController {

    private final ConciergeService concierge;

    public ConciergeController(ConciergeService concierge) {
        this.concierge = concierge;
    }

    @GetMapping("/status")
    public ConciergeStatus status() {
        CurrentUser.require();
        return concierge.status();
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest req) {
        return concierge.chat(CurrentUser.require(), req);
    }

    @GetMapping("/history")
    public List<HistoryItem> history() {
        return concierge.history(CurrentUser.require().getUserId());
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clear() {
        concierge.clearHistory(CurrentUser.require().getUserId());
        return ResponseEntity.noContent().build();
    }
}
