package com.confession.game.domain.player.dto;

import com.confession.game.domain.player.entity.Player;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDto {
    private String id;
    private String name;

    public static PlayerDto from(Player player) {
        return PlayerDto.builder()
                .id(player.getId())
                .name(player.getName())
                .build();
    }
}
