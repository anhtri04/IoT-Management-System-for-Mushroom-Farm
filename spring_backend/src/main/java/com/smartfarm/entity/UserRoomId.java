package com.smartfarm.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key class for UserRoom entity.
 * Represents the combination of user ID and room ID.
 */
public class UserRoomId implements Serializable {

    private UUID user;
    private UUID room;

    // Default constructor
    public UserRoomId() {}

    public UserRoomId(UUID user, UUID room) {
        this.user = user;
        this.room = room;
    }

    // Getters and Setters
    public UUID getUser() {
        return user;
    }

    public void setUser(UUID user) {
        this.user = user;
    }

    public UUID getRoom() {
        return room;
    }

    public void setRoom(UUID room) {
        this.room = room;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserRoomId that = (UserRoomId) o;
        return Objects.equals(user, that.user) && Objects.equals(room, that.room);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, room);
    }

    @Override
    public String toString() {
        return "UserRoomId{" +
                "user=" + user +
                ", room=" + room +
                '}';
    }
}