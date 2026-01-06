package com.confession.game.domain.confession.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Confession {
    private String id;
    private String senderId;  // 서버에서만 관리 (익명성 보장)
    private String message;
    private String explanation;
    private LocalDateTime timestamp;

    public void addExplanation(String explanation) {
        this.explanation = explanation;
    }
}
