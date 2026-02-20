package com.example.gitoo.room;

public record RoomResponse(
        String id,
        String title,
        int now,
        int max,
        boolean locked
) {
    public static RoomResponse from(Room r) {
        return new RoomResponse(r.getId(), r.getTitle(), r.getNowPlayers(), r.getMaxPlayers(), r.isLocked());
    }
}