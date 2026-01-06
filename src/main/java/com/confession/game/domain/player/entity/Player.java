package com.confession.game.domain.player.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String id;
    private String name;
    private String sessionId;

    public void updateSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}