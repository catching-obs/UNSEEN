
package com.confession.game.integration;

import com.confession.game.domain.confession.entity.Confession;
import com.confession.game.domain.player.entity.Player;
import com.confession.game.domain.room.entity.Room;
import com.confession.game.domain.room.repository.RoomRepository;
import com.confession.game.domain.room.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@DisplayName("게임 플로우 통합 테스트")
class GameFlowIntegrationTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomRepository roomRepository;

    private String roomId;

    @BeforeEach
    void setUp() {
        roomId = "integration-test-room-" + System.currentTimeMillis();
    }

    @Test
    @DisplayName("전체 게임 플로우: 참가 -> 게임 시작 -> 고해성사 -> 해명 -> 투표 -> 다음 대상 선택")
    void completeGameFlow() {
        // 1. 플레이어 참가
        Player player1 = roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        Player player2 = roomService.joinRoom(roomId, "player-2", "플레이어2", "session-2");
        Player player3 = roomService.joinRoom(roomId, "player-3", "플레이어3", "session-3");

        assertThat(player1).isNotNull();
        assertThat(player2).isNotNull();
        assertThat(player3).isNotNull();

        Room room = roomService.getRoom(roomId);
        assertThat(room.getPlayers()).hasSize(3);
        assertThat(room.getGameState()).isEqualTo(Room.GameState.WAITING);

        // 2. 게임 시작
        roomService.startGame(roomId);
        room = roomService.getRoom(roomId);
        assertThat(room.getGameState()).isEqualTo(Room.GameState.PLAYING);
        assertThat(room.getCurrentTarget()).isNotNull();

        String currentTarget = room.getCurrentTarget();
        String sender = room.getPlayers().keySet().stream()
                .filter(id -> !id.equals(currentTarget))
                .findFirst()
                .orElseThrow();

        // 3. 고해성사 전송
        Confession confession = roomService.sendConfession(roomId, sender, "당신은 항상 늦어요");
        assertThat(confession).isNotNull();
        assertThat(confession.getMessage()).isEqualTo("당신은 항상 늦어요");

        room = roomService.getRoom(roomId);
        assertThat(room.getConfessions()).hasSize(1);

        // 4. 해명 전송
        roomService.sendExplanation(roomId, currentTarget, confession.getId(), "교통이 막혔어요");
        room = roomService.getRoom(roomId);
        assertThat(room.getConfessions().get(0).getExplanation()).isEqualTo("교통이 막혔어요");

        // 5. 투표
        for (String playerId : room.getPlayers().keySet()) {
            if (!playerId.equals(currentTarget)) {
                Room.VoteResult result = roomService.vote(roomId, playerId, true);
                if (result.isComplete()) {
                    assertThat(result.isAllAgree()).isTrue();
                }
            }
        }

        // 6. 다음 대상 선택
        String nextTarget = room.getPlayers().keySet().stream()
                .filter(id -> !id.equals(currentTarget))
                .findFirst()
                .orElseThrow();

        roomService.selectNextTarget(roomId, currentTarget, nextTarget);
        room = roomService.getRoom(roomId);
        assertThat(room.getCurrentTarget()).isEqualTo(nextTarget);
        assertThat(room.getConfessions()).isEmpty();
        assertThat(room.getVotes()).isEmpty();
    }

    @Test
    @DisplayName("다중 고해성사 시나리오")
    void multipleConfessionsScenario() {
        // given
        roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        roomService.joinRoom(roomId, "player-2", "플레이어2", "session-2");
        roomService.joinRoom(roomId, "player-3", "플레이어3", "session-3");
        roomService.startGame(roomId);

        Room room = roomService.getRoom(roomId);
        String currentTarget = room.getCurrentTarget();

        // when - 여러 고해성사 전송
        for (String playerId : room.getPlayers().keySet()) {
            if (!playerId.equals(currentTarget)) {
                roomService.sendConfession(roomId, playerId, playerId + "의 고해성사");
            }
        }

        // then
        room = roomService.getRoom(roomId);
        assertThat(room.getConfessions()).hasSize(2);
    }

    @Test
    @DisplayName("플레이어 나가기 시나리오")
    void playerLeaveScenario() {
        // given
        roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        roomService.joinRoom(roomId, "player-2", "플레이어2", "session-2");
        roomService.joinRoom(roomId, "player-3", "플레이어3", "session-3");
        roomService.startGame(roomId);

        Room room = roomService.getRoom(roomId);
        String currentTarget = room.getCurrentTarget();

        // when - 대상자가 아닌 플레이어가 나감
        String leavingPlayer = room.getPlayers().keySet().stream()
                .filter(id -> !id.equals(currentTarget))
                .findFirst()
                .orElseThrow();
        roomService.leaveRoom(roomId, leavingPlayer);

        // then
        room = roomService.getRoom(roomId);
        assertThat(room.getPlayers()).hasSize(2);
        assertThat(room.getGameState()).isEqualTo(Room.GameState.PLAYING);
        assertThat(room.getCurrentTarget()).isEqualTo(currentTarget);
    }

    @Test
    @DisplayName("대상자가 나가면 게임이 리셋되는 시나리오")
    void targetLeavesScenario() {
        // given
        roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        roomService.joinRoom(roomId, "player-2", "플레이어2", "session-2");
        roomService.startGame(roomId);

        Room room = roomService.getRoom(roomId);
        String currentTarget = room.getCurrentTarget();

        // when - 대상자가 나감
        roomService.leaveRoom(roomId, currentTarget);

        // then
        room = roomService.getRoom(roomId);
        assertThat(room.getPlayers()).hasSize(1);
        assertThat(room.getGameState()).isEqualTo(Room.GameState.WAITING);
        assertThat(room.getCurrentTarget()).isNull();
    }

    @Test
    @DisplayName("투표 만장일치 시나리오")
    void unanimousVoteScenario() {
        // given
        roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        roomService.joinRoom(roomId, "player-2", "플레이어2", "session-2");
        roomService.joinRoom(roomId, "player-3", "플레이어3", "session-3");
        roomService.joinRoom(roomId, "player-4", "플레이어4", "session-4");
        roomService.startGame(roomId);

        Room room = roomService.getRoom(roomId);
        String currentTarget = room.getCurrentTarget();

        // when - 모든 플레이어가 찬성
        Room.VoteResult finalResult = null;
        for (String playerId : room.getPlayers().keySet()) {
            if (!playerId.equals(currentTarget)) {
                finalResult = roomService.vote(roomId, playerId, true);
            }
        }

        // then
        assertThat(finalResult).isNotNull();
        assertThat(finalResult.isComplete()).isTrue();
        assertThat(finalResult.isAllAgree()).isTrue();
        assertThat(finalResult.getVotes()).isEqualTo(3); // 4명 중 대상 제외 3명
        assertThat(finalResult.getRequired()).isEqualTo(3);
    }

    @Test
    @DisplayName("투표 반대 시나리오")
    void disagreeVoteScenario() {
        // given
        roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        roomService.joinRoom(roomId, "player-2", "플레이어2", "session-2");
        roomService.joinRoom(roomId, "player-3", "플레이어3", "session-3");
        roomService.startGame(roomId);

        Room room = roomService.getRoom(roomId);
        String currentTarget = room.getCurrentTarget();

        // when - 일부 플레이어가 반대
        Room.VoteResult finalResult = null;
        int count = 0;
        for (String playerId : room.getPlayers().keySet()) {
            if (!playerId.equals(currentTarget)) {
                boolean agree = count == 0; // 첫 번째는 찬성, 두 번째는 반대
                finalResult = roomService.vote(roomId, playerId, agree);
                count++;
            }
        }

        // then
        assertThat(finalResult).isNotNull();
        assertThat(finalResult.isComplete()).isTrue();
        assertThat(finalResult.isAllAgree()).isFalse();
    }

    @Test
    @DisplayName("재접속 시나리오")
    void reconnectionScenario() {
        // given
        Player player1 = roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        assertThat(player1.getSessionId()).isEqualTo("session-1");

        // when - 같은 플레이어가 새 세션으로 재접속
        Player reconnectedPlayer = roomService.joinRoom(roomId, "player-1", "플레이어1", "session-2");

        // then
        assertThat(reconnectedPlayer.getSessionId()).isEqualTo("session-2");
        Room room = roomService.getRoom(roomId);
        assertThat(room.getPlayers()).hasSize(1); // 중복 생성 안됨
    }

    @Test
    @DisplayName("빈 방 자동 삭제 시나리오")
    void emptyRoomDeletionScenario() {
        // given
        roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        roomService.joinRoom(roomId, "player-2", "플레이어2", "session-2");

        Room room = roomService.getRoom(roomId);
        assertThat(room.getPlayers()).hasSize(2);

        // when - 모든 플레이어가 나감
        roomService.leaveRoom(roomId, "player-1");
        roomService.leaveRoom(roomId, "player-2");

        // then - 방이 삭제됨
        assertThat(roomRepository.findById(roomId)).isEmpty();
    }

    @Test
    @DisplayName("순환 대상 선택 시나리오")
    void circularTargetSelectionScenario() {
        // given
        roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        roomService.joinRoom(roomId, "player-2", "플레이어2", "session-2");
        roomService.joinRoom(roomId, "player-3", "플레이어3", "session-3");
        roomService.startGame(roomId);

        Room room = roomService.getRoom(roomId);
        String firstTarget = room.getCurrentTarget();

        // when - 모든 플레이어를 대상으로 선택
        String secondTarget = room.getPlayers().keySet().stream()
                .filter(id -> !id.equals(firstTarget))
                .findFirst()
                .orElseThrow();
        roomService.selectNextTarget(roomId, firstTarget, secondTarget);

        Room roomAfterSecond = roomService.getRoom(roomId);
        String thirdTarget = roomAfterSecond.getPlayers().keySet().stream()
                .filter(id -> !roomAfterSecond.getTargetHistory().contains(id))
                .findFirst()
                .orElseThrow();
        roomService.selectNextTarget(roomId, secondTarget, thirdTarget);

        // then
        room = roomService.getRoom(roomId);
        assertThat(room.getTargetHistory()).hasSize(3);
        assertThat(room.getTargetHistory()).containsExactly(firstTarget, secondTarget, thirdTarget);

        // 모든 플레이어가 대상이 되었으므로 더 이상 선택 불가
        Room finalRoom = room;
        assertThatThrownBy(() -> {
            String nextTarget = finalRoom.getPlayers().keySet().stream()
                    .filter(id -> !finalRoom.getTargetHistory().contains(id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No more available targets"));
        }).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("동시 투표 시나리오")
    void concurrentVoteScenario() throws InterruptedException {
        // given
        for (int i = 1; i <= 10; i++) {
            roomService.joinRoom(roomId, "player-" + i, "플레이어" + i, "session-" + i);
        }
        roomService.startGame(roomId);

        Room room = roomService.getRoom(roomId);
        String currentTarget = room.getCurrentTarget();

        // when - 동시에 투표
        Thread[] threads = room.getPlayers().keySet().stream()
                .filter(id -> !id.equals(currentTarget))
                .map(playerId -> new Thread(() -> roomService.vote(roomId, playerId, true)))
                .toArray(Thread[]::new);

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // then
        room = roomService.getRoom(roomId);
        assertThat(room.getVotes()).hasSize(9); // 10명 중 대상 제외 9명
    }

    @Test
    @DisplayName("예외 상황: 대상자가 고해성사 전송 시도")
    void exceptionTargetSendsConfession() {
        // given
        roomService.joinRoom(roomId, "player-1", "플레이어1", "session-1");
        roomService.joinRoom(roomId, "player-2", "플레이어2", "session-2");
        roomService.startGame(roomId);

        Room room = roomService.getRoom(roomId);
        String currentTarget = room.getCurrentTarget();

        // when & then - 대상자가 고해성사를 보내려고 시도
        assertThatThrownBy(() -> roomService.sendConfession(roomId, currentTarget, "메시지"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("대상자는 고해성사 메시지를 보낼 수 없습니다.");
    }

    @Test
    @DisplayName("예외 상황: 존재하지 않는 방에 접근")
    void exceptionNonexistentRoom() {
        // when & then
        assertThatThrownBy(() -> roomService.getRoom("nonexistent-room"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("방을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("성능 테스트: 100명의 플레이어 처리")
    void performanceTest100Players() {
        // given
        long startTime = System.currentTimeMillis();

        // when - 100명 참가
        for (int i = 1; i <= 100; i++) {
            roomService.joinRoom(roomId, "player-" + i, "플레이어" + i, "session-" + i);
        }

        long joinTime = System.currentTimeMillis() - startTime;

        // 게임 시작
        startTime = System.currentTimeMillis();
        roomService.startGame(roomId);
        long startGameTime = System.currentTimeMillis() - startTime;

        // then
        Room room = roomService.getRoom(roomId);
        assertThat(room.getPlayers()).hasSize(100);
        assertThat(room.getCurrentTarget()).isNotNull();

        System.out.println("100명 참가 소요 시간: " + joinTime + "ms");
        System.out.println("게임 시작 소요 시간: " + startGameTime + "ms");

        // 일반적으로 1초 이내에 완료되어야 함
        assertThat(joinTime).isLessThan(1000);
        assertThat(startGameTime).isLessThan(100);
    }
}
