package com.confession.game.domain.room.entity;

import com.confession.game.domain.confession.entity.Confession;
import com.confession.game.domain.player.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Room 엔티티 테스트")
class RoomTest {

    private Room room;
    private String roomId;

    @BeforeEach
    void setUp() {
        roomId = "test-room";
        room = Room.builder().roomId(roomId).build();
    }

    @Nested
    @DisplayName("addPlayer 테스트")
    class AddPlayerTest {

        @Test
        @DisplayName("새로운 플레이어를 추가한다")
        void addNewPlayer() {
            // when
            Player player = room.addPlayer("player-1", "플레이어1", "session-1");

            // then
            assertThat(player).isNotNull();
            assertThat(player.getId()).isEqualTo("player-1");
            assertThat(player.getName()).isEqualTo("플레이어1");
            assertThat(player.getSessionId()).isEqualTo("session-1");
            assertThat(room.getPlayers()).hasSize(1);
            assertThat(room.getPlayers()).containsKey("player-1");
        }

        @Test
        @DisplayName("기존 플레이어가 재접속하면 세션ID만 업데이트된다")
        void updateSessionIdForExistingPlayer() {
            // given
            room.addPlayer("player-1", "플레이어1", "old-session");

            // when
            Player player = room.addPlayer("player-1", "플레이어1", "new-session");

            // then
            assertThat(player.getSessionId()).isEqualTo("new-session");
            assertThat(room.getPlayers()).hasSize(1);
        }

        @Test
        @DisplayName("여러 플레이어를 추가할 수 있다")
        void addMultiplePlayers() {
            // when
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.addPlayer("player-3", "플레이어3", "session-3");

            // then
            assertThat(room.getPlayers()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("removePlayer 테스트")
    class RemovePlayerTest {

        @Test
        @DisplayName("플레이어를 제거한다")
        void removePlayer() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");

            // when
            room.removePlayer("player-1");

            // then
            assertThat(room.getPlayers()).hasSize(1);
            assertThat(room.getPlayers()).doesNotContainKey("player-1");
        }

        @Test
        @DisplayName("대상자를 제거하면 게임이 리셋된다")
        void resetGameWhenTargetRemoved() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();
            String currentTarget = room.getCurrentTarget();
            room.addConfession("player-1", "고해성사");

            // when
            room.removePlayer(currentTarget);

            // then
            assertThat(room.getGameState()).isEqualTo(Room.GameState.WAITING);
            assertThat(room.getCurrentTarget()).isNull();
            assertThat(room.getConfessions()).isEmpty();
            assertThat(room.getVotes()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 플레이어를 제거해도 에러가 발생하지 않는다")
        void removeNonexistentPlayer() {
            // when & then
            assertThatCode(() -> room.removePlayer("nonexistent"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("startGame 테스트")
    class StartGameTest {

        @Test
        @DisplayName("게임을 시작하면 상태가 PLAYING으로 변경되고 대상자가 선택된다")
        void startGame() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");

            // when
            room.startGame();

            // then
            assertThat(room.getGameState()).isEqualTo(Room.GameState.PLAYING);
            assertThat(room.getCurrentTarget()).isNotNull();
            assertThat(room.getTargetHistory()).hasSize(1);
            assertThat(room.getTargetHistory()).contains(room.getCurrentTarget());
        }

        @Test
        @DisplayName("플레이어가 2명 미만이면 예외를 발생시킨다")
        void throwExceptionWhenNotEnoughPlayers() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");

            // when & then
            assertThatThrownBy(() -> room.startGame())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("최소 2명 이상의 플레이어가 필요합니다.");
        }

        @Test
        @DisplayName("플레이어가 없으면 예외를 발생시킨다")
        void throwExceptionWhenNoPlayers() {
            // when & then
            assertThatThrownBy(() -> room.startGame())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("최소 2명 이상의 플레이어가 필요합니다.");
        }

        @Test
        @DisplayName("모든 플레이어가 대상이 되면 히스토리를 초기화하고 다시 선택한다")
        void resetHistoryWhenAllPlayersSelected() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String firstTarget = room.getCurrentTarget();
            String secondTarget = room.getPlayers().keySet().stream()
                    .filter(id -> !id.equals(firstTarget))
                    .findFirst()
                    .orElseThrow();

            room.selectNextTarget(secondTarget);

            // when - 모든 플레이어가 대상이 되었으므로 히스토리 초기화
            String thirdTarget = room.getPlayers().keySet().stream()
                    .filter(id -> !room.getTargetHistory().contains(id))
                    .findFirst()
                    .orElse(null);

            // then - 더 이상 선택 가능한 플레이어가 없음
            assertThat(thirdTarget).isNull();
            assertThat(room.getTargetHistory()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("selectNextTarget 테스트")
    class SelectNextTargetTest {

        @Test
        @DisplayName("다음 대상을 선택한다")
        void selectNextTarget() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.addPlayer("player-3", "플레이어3", "session-3");
            room.startGame();

            String firstTarget = room.getCurrentTarget();
            String nextTarget = room.getPlayers().keySet().stream()
                    .filter(id -> !id.equals(firstTarget))
                    .findFirst()
                    .orElseThrow();

            room.addConfession("sender", "고해성사");
            room.vote("voter", true);

            // when
            room.selectNextTarget(nextTarget);

            // then
            assertThat(room.getCurrentTarget()).isEqualTo(nextTarget);
            assertThat(room.getTargetHistory()).hasSize(2);
            assertThat(room.getTargetHistory()).contains(firstTarget, nextTarget);
            assertThat(room.getConfessions()).isEmpty();
            assertThat(room.getVotes()).isEmpty();
        }

        @Test
        @DisplayName("이미 대상이 된 플레이어를 선택하면 예외를 발생시킨다")
        void throwExceptionWhenSelectingPreviousTarget() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String currentTarget = room.getCurrentTarget();

            // when & then
            assertThatThrownBy(() -> room.selectNextTarget(currentTarget))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 고해성사 대상이 된 플레이어입니다.");
        }

        @Test
        @DisplayName("존재하지 않는 플레이어를 선택하면 예외를 발생시킨다")
        void throwExceptionWhenSelectingNonexistentPlayer() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            // when & then
            assertThatThrownBy(() -> room.selectNextTarget("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 플레이어입니다.");
        }
    }

    @Nested
    @DisplayName("addConfession 테스트")
    class AddConfessionTest {

        @Test
        @DisplayName("고해성사를 추가한다")
        void addConfession() {
            // when
            Confession confession = room.addConfession("player-1", "테스트 메시지");

            // then
            assertThat(confession).isNotNull();
            assertThat(confession.getId()).isNotNull();
            assertThat(confession.getSenderId()).isEqualTo("player-1");
            assertThat(confession.getMessage()).isEqualTo("테스트 메시지");
            assertThat(confession.getTimestamp()).isNotNull();
            assertThat(room.getConfessions()).hasSize(1);
        }

        @Test
        @DisplayName("여러 개의 고해성사를 추가할 수 있다")
        void addMultipleConfessions() {
            // when
            room.addConfession("player-1", "메시지1");
            room.addConfession("player-2", "메시지2");
            room.addConfession("player-3", "메시지3");

            // then
            assertThat(room.getConfessions()).hasSize(3);
        }

        @Test
        @DisplayName("각 고해성사는 고유한 ID를 가진다")
        void eachConfessionHasUniqueId() {
            // when
            Confession confession1 = room.addConfession("player-1", "메시지1");
            Confession confession2 = room.addConfession("player-1", "메시지2");

            // then
            assertThat(confession1.getId()).isNotEqualTo(confession2.getId());
        }
    }

    @Nested
    @DisplayName("addExplanation 테스트")
    class AddExplanationTest {

        @Test
        @DisplayName("해명을 추가한다")
        void addExplanation() {
            // given
            Confession confession = room.addConfession("player-1", "고해성사");

            // when
            room.addExplanation(confession.getId(), "해명입니다");

            // then
            assertThat(confession.getExplanation()).isEqualTo("해명입니다");
        }

        @Test
        @DisplayName("존재하지 않는 고해성사 ID로 해명을 추가하면 예외를 발생시킨다")
        void throwExceptionWhenConfessionNotFound() {
            // when & then
            assertThatThrownBy(() -> room.addExplanation("invalid-id", "해명"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("고해성사 메시지를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("vote 테스트")
    class VoteTest {

        @Test
        @DisplayName("투표를 진행한다")
        void vote() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            // when
            Room.VoteResult result = room.vote("voter-1", true);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getVotes()).isEqualTo(1);
            assertThat(result.getRequired()).isEqualTo(1); // 2명 중 대상 제외 1명
            assertThat(result.isComplete()).isTrue();
            assertThat(result.isAllAgree()).isTrue();
        }

        @Test
        @DisplayName("동일한 플레이어가 다시 투표하면 덮어쓴다")
        void overwriteVote() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            // when
            room.vote("voter-1", true);
            Room.VoteResult result = room.vote("voter-1", false);

            // then
            assertThat(result.getVotes()).isEqualTo(1);
            assertThat(result.isAllAgree()).isFalse();
        }

        @Test
        @DisplayName("모든 플레이어가 투표하면 완료된다")
        void voteComplete() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.addPlayer("player-3", "플레이어3", "session-3");
            room.startGame();

            // when
            room.vote("voter-1", true);
            Room.VoteResult result = room.vote("voter-2", true);

            // then
            assertThat(result.isComplete()).isTrue();
            assertThat(result.getVotes()).isEqualTo(2);
            assertThat(result.getRequired()).isEqualTo(2); // 3명 중 대상 제외 2명
        }

        @Test
        @DisplayName("한 명이라도 반대하면 전체 동의가 아니다")
        void notAllAgree() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.addPlayer("player-3", "플레이어3", "session-3");
            room.startGame();

            // when
            room.vote("voter-1", true);
            Room.VoteResult result = room.vote("voter-2", false);

            // then
            assertThat(result.isComplete()).isTrue();
            assertThat(result.isAllAgree()).isFalse();
        }

        @Test
        @DisplayName("아무도 투표하지 않으면 미완료 상태이다")
        void notComplete() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.addPlayer("player-3", "플레이어3", "session-3");
            room.startGame();

            // when
            Room.VoteResult result = room.vote("voter-1", true);

            // then
            assertThat(result.isComplete()).isFalse();
            assertThat(result.getVotes()).isEqualTo(1);
            assertThat(result.getRequired()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("resetGame 테스트")
    class ResetGameTest {

        @Test
        @DisplayName("게임을 리셋한다")
        void resetGame() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();
            room.addConfession("player-1", "고해성사");
            room.vote("player-1", true);

            // when
            room.resetGame();

            // then
            assertThat(room.getGameState()).isEqualTo(Room.GameState.WAITING);
            assertThat(room.getCurrentTarget()).isNull();
            assertThat(room.getConfessions()).isEmpty();
            assertThat(room.getVotes()).isEmpty();
        }

        @Test
        @DisplayName("리셋 후에도 플레이어와 히스토리는 유지된다")
        void keepPlayersAndHistoryAfterReset() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            int playerCount = room.getPlayers().size();
            int historySize = room.getTargetHistory().size();

            // when
            room.resetGame();

            // then
            assertThat(room.getPlayers()).hasSize(playerCount);
            assertThat(room.getTargetHistory()).hasSize(historySize);
        }
    }

    @Nested
    @DisplayName("isEmpty 테스트")
    class IsEmptyTest {

        @Test
        @DisplayName("플레이어가 없으면 true를 반환한다")
        void returnTrueWhenNoPlayers() {
            // when & then
            assertThat(room.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("플레이어가 있으면 false를 반환한다")
        void returnFalseWhenHasPlayers() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");

            // when & then
            assertThat(room.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("모든 플레이어를 제거하면 true를 반환한다")
        void returnTrueAfterRemovingAllPlayers() {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");

            // when
            room.removePlayer("player-1");
            room.removePlayer("player-2");

            // then
            assertThat(room.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTest {

        @Test
        @DisplayName("여러 스레드에서 동시에 플레이어를 추가해도 안전하다")
        void concurrentAddPlayer() throws InterruptedException {
            // given
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            // when
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    room.addPlayer("player-" + index, "플레이어" + index, "session-" + index);
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // then
            assertThat(room.getPlayers()).hasSize(threadCount);
        }

        @Test
        @DisplayName("여러 스레드에서 동시에 투표해도 안전하다")
        void concurrentVote() throws InterruptedException {
            // given
            room.addPlayer("player-1", "플레이어1", "session-1");
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            // when
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    room.vote("voter-" + index, true);
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // then
            assertThat(room.getVotes()).hasSize(threadCount);
        }
    }
}
