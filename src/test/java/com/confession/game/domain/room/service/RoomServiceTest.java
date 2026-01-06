package com.confession.game.domain.room.service;

import com.confession.game.domain.confession.entity.Confession;
import com.confession.game.domain.player.entity.Player;
import com.confession.game.domain.room.entity.Room;
import com.confession.game.domain.room.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService 테스트")
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    private String testRoomId;
    private String testPlayerId;
    private String testPlayerName;
    private String testSessionId;

    @BeforeEach
    void setUp() {
        testRoomId = "test-room-1";
        testPlayerId = "player-1";
        testPlayerName = "테스트플레이어";
        testSessionId = "session-1";
    }

    @Nested
    @DisplayName("getOrCreateRoom 테스트")
    class GetOrCreateRoomTest {

        @Test
        @DisplayName("기존 방이 존재하면 해당 방을 반환한다")
        void returnExistingRoom() {
            // given
            Room existingRoom = Room.builder().roomId(testRoomId).build();
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(existingRoom));

            // when
            Room result = roomService.getOrCreateRoom(testRoomId);

            // then
            assertThat(result).isEqualTo(existingRoom);
            verify(roomRepository, never()).save(any());
        }

        @Test
        @DisplayName("방이 존재하지 않으면 새로운 방을 생성한다")
        void createNewRoom() {
            // given
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.empty());
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Room result = roomService.getOrCreateRoom(testRoomId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getRoomId()).isEqualTo(testRoomId);
            verify(roomRepository).save(any(Room.class));
        }
    }

    @Nested
    @DisplayName("getRoom 테스트")
    class GetRoomTest {

        @Test
        @DisplayName("방이 존재하면 해당 방을 반환한다")
        void returnRoom() {
            // given
            Room existingRoom = Room.builder().roomId(testRoomId).build();
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(existingRoom));

            // when
            Room result = roomService.getRoom(testRoomId);

            // then
            assertThat(result).isEqualTo(existingRoom);
        }

        @Test
        @DisplayName("방이 존재하지 않으면 예외를 발생시킨다")
        void throwExceptionWhenRoomNotFound() {
            // given
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> roomService.getRoom(testRoomId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("방을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("joinRoom 테스트")
    class JoinRoomTest {

        @Test
        @DisplayName("새로운 플레이어가 방에 참가한다")
        void joinNewPlayer() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Player result = roomService.joinRoom(testRoomId, testPlayerId, testPlayerName, testSessionId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testPlayerId);
            assertThat(result.getName()).isEqualTo(testPlayerName);
            assertThat(result.getSessionId()).isEqualTo(testSessionId);
            verify(roomRepository).save(any(Room.class));
        }

        @Test
        @DisplayName("기존 플레이어가 재접속하면 세션ID만 업데이트된다")
        void rejoinExistingPlayer() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, "old-session");
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            String newSessionId = "new-session";

            // when
            Player result = roomService.joinRoom(testRoomId, testPlayerId, testPlayerName, newSessionId);

            // then
            assertThat(result.getSessionId()).isEqualTo(newSessionId);
            assertThat(room.getPlayers()).hasSize(1);
        }

        @Test
        @DisplayName("방이 없으면 새로운 방을 생성하고 참가한다")
        void joinAndCreateRoom() {
            // given
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.empty());
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Player result = roomService.joinRoom(testRoomId, testPlayerId, testPlayerName, testSessionId);

            // then
            assertThat(result).isNotNull();
            verify(roomRepository, times(2)).save(any(Room.class)); // create + join
        }
    }

    @Nested
    @DisplayName("leaveRoom 테스트")
    class LeaveRoomTest {

        @Test
        @DisplayName("플레이어가 방을 나간다")
        void leaveRoom() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            roomService.leaveRoom(testRoomId, testPlayerId);

            // then
            assertThat(room.getPlayers()).doesNotContainKey(testPlayerId);
            verify(roomRepository).save(room);
            verify(roomRepository, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("마지막 플레이어가 나가면 방이 삭제된다")
        void deleteRoomWhenEmpty() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));

            // when
            roomService.leaveRoom(testRoomId, testPlayerId);

            // then
            verify(roomRepository).deleteById(testRoomId);
            verify(roomRepository, never()).save(any());
        }

        @Test
        @DisplayName("대상자가 나가면 게임이 리셋된다")
        void resetGameWhenTargetLeaves() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();
            String currentTarget = room.getCurrentTarget();
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            roomService.leaveRoom(testRoomId, currentTarget);

            // then
            assertThat(room.getGameState()).isEqualTo(Room.GameState.WAITING);
            assertThat(room.getCurrentTarget()).isNull();
        }
    }

    @Nested
    @DisplayName("startGame 테스트")
    class StartGameTest {

        @Test
        @DisplayName("게임을 정상적으로 시작한다")
        void startGame() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            roomService.startGame(testRoomId);

            // then
            assertThat(room.getGameState()).isEqualTo(Room.GameState.PLAYING);
            assertThat(room.getCurrentTarget()).isNotNull();
            verify(roomRepository).save(room);
        }

        @Test
        @DisplayName("플레이어가 2명 미만이면 예외를 발생시킨다")
        void throwExceptionWhenNotEnoughPlayers() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.startGame(testRoomId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("최소 2명 이상의 플레이어가 필요합니다.");
        }
    }

    @Nested
    @DisplayName("sendConfession 테스트")
    class SendConfessionTest {

        @Test
        @DisplayName("고해성사 메시지를 정상적으로 전송한다")
        void sendConfession() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String senderId = room.getCurrentTarget().equals(testPlayerId) ? "player-2" : testPlayerId;
            String message = "테스트 고해성사";

            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Confession result = roomService.sendConfession(testRoomId, senderId, message);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSenderId()).isEqualTo(senderId);
            assertThat(result.getMessage()).isEqualTo(message);
            verify(roomRepository).save(room);
        }

        @Test
        @DisplayName("대상자가 고해성사를 보내면 예외를 발생시킨다")
        void throwExceptionWhenTargetSendsConfession() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String currentTarget = room.getCurrentTarget();
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.sendConfession(testRoomId, currentTarget, "메시지"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("대상자는 고해성사 메시지를 보낼 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("sendExplanation 테스트")
    class SendExplanationTest {

        @Test
        @DisplayName("해명을 정상적으로 전송한다")
        void sendExplanation() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String senderId = room.getCurrentTarget().equals(testPlayerId) ? "player-2" : testPlayerId;
            Confession confession = room.addConfession(senderId, "고해성사");

            String explanation = "해명입니다";
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            roomService.sendExplanation(testRoomId, room.getCurrentTarget(), confession.getId(), explanation);

            // then
            assertThat(confession.getExplanation()).isEqualTo(explanation);
            verify(roomRepository).save(room);
        }

        @Test
        @DisplayName("대상자가 아닌 플레이어가 해명하면 예외를 발생시킨다")
        void throwExceptionWhenNonTargetSendsExplanation() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String nonTarget = room.getCurrentTarget().equals(testPlayerId) ? "player-2" : testPlayerId;
            Confession confession = room.addConfession(nonTarget, "고해성사");

            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.sendExplanation(testRoomId, nonTarget, confession.getId(), "해명"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("대상자만 해명할 수 있습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 고해성사에 해명하면 예외를 발생시킨다")
        void throwExceptionWhenConfessionNotFound() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.sendExplanation(testRoomId, room.getCurrentTarget(), "invalid-id", "해명"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("고해성사 메시지를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("vote 테스트")
    class VoteTest {

        @Test
        @DisplayName("투표를 정상적으로 진행한다")
        void vote() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String voter = room.getCurrentTarget().equals(testPlayerId) ? "player-2" : testPlayerId;
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            Room.VoteResult result = roomService.vote(testRoomId, voter, true);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getVotes()).isEqualTo(1);
            assertThat(result.getRequired()).isEqualTo(1);
            assertThat(result.isComplete()).isTrue();
            assertThat(result.isAllAgree()).isTrue();
            verify(roomRepository).save(room);
        }

        @Test
        @DisplayName("대상자가 투표하면 예외를 발생시킨다")
        void throwExceptionWhenTargetVotes() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String currentTarget = room.getCurrentTarget();
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.vote(testRoomId, currentTarget, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("대상자는 투표할 수 없습니다.");
        }

        @Test
        @DisplayName("모든 플레이어가 동의하면 투표가 완료된다")
        void voteCompleteAllAgree() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.addPlayer("player-3", "플레이어3", "session-3");
            room.startGame();

            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            String currentTarget = room.getCurrentTarget();
            Room.VoteResult result1 = null;
            Room.VoteResult result2 = null;

            for (String playerId : room.getPlayers().keySet()) {
                if (!playerId.equals(currentTarget)) {
                    if (result1 == null) {
                        result1 = roomService.vote(testRoomId, playerId, true);
                    } else {
                        result2 = roomService.vote(testRoomId, playerId, true);
                    }
                }
            }

            // then
            assertThat(result2.isComplete()).isTrue();
            assertThat(result2.isAllAgree()).isTrue();
        }

        @Test
        @DisplayName("한 명이라도 반대하면 전체 동의가 아니다")
        void voteCompleteNotAllAgree() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.addPlayer("player-3", "플레이어3", "session-3");
            room.startGame();

            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            String currentTarget = room.getCurrentTarget();
            Room.VoteResult result1 = null;
            Room.VoteResult result2 = null;

            int count = 0;
            for (String playerId : room.getPlayers().keySet()) {
                if (!playerId.equals(currentTarget)) {
                    if (result1 == null) {
                        result1 = roomService.vote(testRoomId, playerId, true);
                    } else {
                        result2 = roomService.vote(testRoomId, playerId, false);
                    }
                }
            }

            // then
            assertThat(result2.isComplete()).isTrue();
            assertThat(result2.isAllAgree()).isFalse();
        }
    }

    @Nested
    @DisplayName("selectNextTarget 테스트")
    class SelectNextTargetTest {

        @Test
        @DisplayName("다음 대상을 정상적으로 선택한다")
        void selectNextTarget() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.addPlayer("player-3", "플레이어3", "session-3");
            room.startGame();

            String currentTarget = room.getCurrentTarget();
            String nextTarget = room.getPlayers().keySet().stream()
                    .filter(id -> !id.equals(currentTarget) && !room.getTargetHistory().contains(id))
                    .findFirst()
                    .orElseThrow();

            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));
            when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            roomService.selectNextTarget(testRoomId, currentTarget, nextTarget);

            // then
            assertThat(room.getCurrentTarget()).isEqualTo(nextTarget);
            assertThat(room.getTargetHistory()).contains(nextTarget);
            assertThat(room.getConfessions()).isEmpty();
            assertThat(room.getVotes()).isEmpty();
            verify(roomRepository).save(room);
        }

        @Test
        @DisplayName("현재 대상자가 아닌 플레이어가 선택하면 예외를 발생시킨다")
        void throwExceptionWhenNonTargetSelects() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String currentTarget = room.getCurrentTarget();
            String nonTarget = currentTarget.equals(testPlayerId) ? "player-2" : testPlayerId;

            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.selectNextTarget(testRoomId, nonTarget, "player-2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("현재 대상자만 다음 대상을 선택할 수 있습니다.");
        }

        @Test
        @DisplayName("이미 대상이었던 플레이어를 선택하면 예외를 발생시킨다")
        void throwExceptionWhenSelectingPreviousTarget() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String currentTarget = room.getCurrentTarget();
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.selectNextTarget(testRoomId, currentTarget, currentTarget))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 고해성사 대상이 된 플레이어입니다.");
        }

        @Test
        @DisplayName("존재하지 않는 플레이어를 선택하면 예외를 발생시킨다")
        void throwExceptionWhenSelectingNonexistentPlayer() {
            // given
            Room room = Room.builder().roomId(testRoomId).build();
            room.addPlayer(testPlayerId, testPlayerName, testSessionId);
            room.addPlayer("player-2", "플레이어2", "session-2");
            room.startGame();

            String currentTarget = room.getCurrentTarget();
            when(roomRepository.findById(testRoomId)).thenReturn(Optional.of(room));

            // when & then
            assertThatThrownBy(() -> roomService.selectNextTarget(testRoomId, currentTarget, "nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 플레이어입니다.");
        }
    }
}