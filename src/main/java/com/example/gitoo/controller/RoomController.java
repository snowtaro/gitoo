package com.example.gitoo.controller;

import com.example.gitoo.dto.request.CreateRoomRequest;
import com.example.gitoo.dto.request.JoinRoomRequest;
import com.example.gitoo.dto.request.ReadyRequest;
import com.example.gitoo.dto.response.RoomDetailResponse;
import com.example.gitoo.dto.response.RoomResponse;
import com.example.gitoo.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public List<RoomResponse> list() {
        return roomService.list();
    }

    @GetMapping("/{roomId}")
    public RoomDetailResponse detail(@PathVariable String roomId) {
        return roomService.detail(roomId);
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest req) {
        return ResponseEntity.ok(roomService.create(req));
    }

    @PostMapping("/{roomId}/join")
    public RoomDetailResponse join(@PathVariable String roomId,
                                   @RequestBody(required = false) JoinRoomRequest req,
                                   Authentication auth) {
        return roomService.join(roomId, req, auth.getName());
    }
    @PostMapping("/{roomId}/leave")
    public RoomDetailResponse leave(
            @PathVariable String roomId,
            Authentication auth
    ){
        return roomService.leave(roomId,auth.getName());
    }
    @PostMapping("/{roomId}/ready")
    public RoomDetailResponse toggleReady(@PathVariable String roomId, Authentication auth){
        return roomService.toggleReady(roomId,auth.getName());
    }

    @PostMapping("/{roomId}/start")
    public void start(
            @PathVariable String roomId,
            Authentication authentication
    ) {
        roomService.start(roomId, authentication.getName());
    }

}