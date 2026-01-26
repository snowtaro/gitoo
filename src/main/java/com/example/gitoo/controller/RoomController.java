package com.example.gitoo.controller;

import com.example.gitoo.dto.request.CreateRoomRequest;
import com.example.gitoo.dto.request.JoinRoomRequest;
import com.example.gitoo.dto.response.RoomResponse;
import com.example.gitoo.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/rooms")
    public List<RoomResponse> list() {
        return roomService.list();
    }

    @PostMapping("/rooms")
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest req) {
        return ResponseEntity.ok(roomService.create(req));
    }

    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<Void> join(@PathVariable String roomId, @RequestBody(required = false) JoinRoomRequest req) {
        roomService.join(roomId, req);
        return ResponseEntity.ok().build();
    }
}
