package backend.controller;

import backend.dto.ConciergeDtos.MeetingDto;
import backend.dto.ConciergeDtos.MeetingUpdateDto;
import backend.dto.ConciergeDtos.MeetingUpdateRequest;
import backend.dto.ConciergeDtos.MessageDto;
import backend.dto.ConciergeDtos.MessageRequest;
import backend.dto.ConciergeDtos.MyShare;
import backend.dto.ConciergeDtos.ShareRequest;
import backend.security.CurrentUser;
import backend.service.MeetingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Both parties of a request use the same endpoints; MeetingService checks they are a party. */
@RestController
@RequestMapping("/api/orders/{id}")
public class MeetingController {

    private final MeetingService meetings;

    public MeetingController(MeetingService meetings) {
        this.meetings = meetings;
    }

    @GetMapping("/meeting")
    public MeetingDto meeting(@PathVariable("id") Long id) {
        return meetings.view(CurrentUser.require(), id);
    }

    @PostMapping("/meeting/updates")
    public ResponseEntity<MeetingUpdateDto> update(@PathVariable("id") Long id,
                                                   @Valid @RequestBody MeetingUpdateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetings.postUpdate(CurrentUser.require(), id, req));
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageDto> send(@PathVariable("id") Long id, @Valid @RequestBody MessageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetings.sendMessage(CurrentUser.require(), id, req.body()));
    }

    @PutMapping("/location-share")
    public MyShare startShare(@PathVariable("id") Long id, @Valid @RequestBody ShareRequest req) {
        return meetings.startOrRefreshShare(CurrentUser.require(), id, req);
    }

    @DeleteMapping("/location-share")
    public MyShare stopShare(@PathVariable("id") Long id) {
        return meetings.stopShare(CurrentUser.require(), id);
    }
}
