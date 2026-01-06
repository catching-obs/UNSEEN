package com.confession.game.domain.confession.dto;

import com.confession.game.domain.confession.entity.Confession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfessionDto {
    private String id;
    private String message;
    private String explanation;
    private LocalDateTime timestamp;

    public static ConfessionDto from(Confession confession) {
        return ConfessionDto.builder()
                .id(confession.getId())
                .message(confession.getMessage())
                .explanation(confession.getExplanation())
                .timestamp(confession.getTimestamp())
                .build();
    }
}