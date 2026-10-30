package backend.controller;

import backend.dto.ConciergeDtos.CaseDto;
import backend.dto.ConciergeDtos.CreateCaseRequest;
import backend.dto.ConciergeDtos.UpdateCaseRequest;
import backend.security.CurrentUser;
import backend.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SupportController {

    private final SupportService support;

    public SupportController(SupportService support) {
        this.support = support;
    }

    @PostMapping("/api/support/cases")
    public ResponseEntity<CaseDto> create(@Valid @RequestBody CreateCaseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(support.create(CurrentUser.require(), req));
    }

    @GetMapping("/api/support/cases/mine")
    public List<CaseDto> mine() {
        return support.mine(CurrentUser.require().getUserId());
    }

    @GetMapping("/api/support/cases/{id}")
    public CaseDto get(@PathVariable("id") Long id) {
        return support.get(CurrentUser.require(), id);
    }

    // ADMIN role is enforced for /api/admin/** in SecurityConfig.
    @GetMapping("/api/admin/support/cases")
    public List<CaseDto> adminList(@RequestParam(value = "status", required = false) String status) {
        return support.adminList(status);
    }

    @PutMapping("/api/admin/support/cases/{id}")
    public CaseDto adminUpdate(@PathVariable("id") Long id, @Valid @RequestBody UpdateCaseRequest req) {
        return support.adminUpdate(CurrentUser.require().getUserId(), id, req);
    }
}
