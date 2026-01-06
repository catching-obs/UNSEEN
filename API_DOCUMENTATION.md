# 고해성사 게임 - WebSocket API 명세서

## 서버 정보
- **서버 주소**: `http://localhost:3000`
- **프로토콜**: Socket.IO
- **실시간 통신**: WebSocket 기반

## 연결 방법

```javascript
import { io } from 'socket.io-client';

const socket = io('http://localhost:3000');
```

---

## 📤 클라이언트 → 서버 이벤트

### 1. 방 참가
**이벤트**: `join-room`

```javascript
socket.emit('join-room', {
  roomId: string,      // 방 ID (예: "room1")
  playerId: string,    // 플레이어 고유 ID (UUID 권장)
  playerName: string   // 플레이어 닉네임
});
```

**응답**: `join-room-success` 이벤트 수신

---

### 2. 게임 시작
**이벤트**: `start-game`

```javascript
socket.emit('start-game', {
  roomId: string  // 방 ID
});
```

**응답**: `game-started` 이벤트를 모든 플레이어가 수신

**주의**: 최소 2명 이상의 플레이어가 필요

---

### 3. 일반 채팅 메시지 전송
**이벤트**: `send-chat-message`

**설명**: 고해성사 대상자를 제외한 나머지 플레이어들만 볼 수 있는 채팅

```javascript
socket.emit('send-chat-message', {
  roomId: string,   // 방 ID
  message: string   // 채팅 메시지
});
```

**응답**: `chat-message` 이벤트를 대상자를 제외한 플레이어들이 수신

---

### 4. 고해성사 메시지 전송
**이벤트**: `send-confession`

**설명**: 대상자에게만 익명으로 전송되는 메시지

```javascript
socket.emit('send-confession', {
  roomId: string,   // 방 ID
  message: string   // 고해성사 메시지
});
```

**응답**:
- 발신자: `confession-sent` 이벤트 수신
- 대상자: `confession-received` 이벤트 수신 (익명)

**주의**: 현재 고해성사 대상자는 이 메시지를 보낼 수 없음

---

### 5. 해명 전송
**이벤트**: `send-explanation`

**설명**: 고해성사 대상자만 받은 메시지에 대해 해명을 작성할 수 있음

```javascript
socket.emit('send-explanation', {
  roomId: string,        // 방 ID
  confessionId: string,  // 고해성사 메시지 ID
  explanation: string    // 해명 내용
});
```

**응답**: `explanation-received` 이벤트를 모든 플레이어가 수신

**주의**: 현재 고해성사 대상자만 해명을 보낼 수 있음

---

### 6. 투표 (동의)
**이벤트**: `vote`

**설명**: 다음 턴으로 넘어가기 위한 동의 투표

```javascript
socket.emit('vote', {
  roomId: string,   // 방 ID
  agree: boolean    // true: 동의, false: 비동의
});
```

**응답**:
- `vote-updated` 이벤트를 모든 플레이어가 수신
- 모든 투표가 완료되면 `vote-complete` 이벤트 수신

**주의**:
- 현재 고해성사 대상자는 투표할 수 없음
- 대상자를 제외한 모든 플레이어가 투표해야 완료됨

---

### 7. 다음 대상자 선택
**이벤트**: `select-next-target`

**설명**: 현재 대상자가 다음 고해성사 대상을 선택

```javascript
socket.emit('select-next-target', {
  roomId: string,   // 방 ID
  targetId: string  // 다음 대상자의 플레이어 ID
});
```

**응답**: `new-target-selected` 이벤트를 모든 플레이어가 수신

**주의**:
- 현재 고해성사 대상자만 다음 대상을 선택할 수 있음
- 이미 대상이 되었던 플레이어는 선택할 수 없음 (중복 불가)
- 모든 플레이어가 한 번씩 대상이 되면 히스토리가 초기화됨

---

### 8. 방 나가기
**이벤트**: `leave-room`

```javascript
socket.emit('leave-room', {
  roomId: string  // 방 ID
});
```

**응답**: `player-list-updated` 이벤트를 남은 플레이어들이 수신

---

## 📥 서버 → 클라이언트 이벤트

### 1. 방 참가 성공
**이벤트**: `join-room-success`

```javascript
socket.on('join-room-success', (data) => {
  console.log(data);
  /*
  {
    player: {
      id: string,
      name: string
    },
    room: {
      roomId: string,
      players: [
        { id: string, name: string },
        ...
      ],
      gameState: 'waiting' | 'playing',
      currentTarget: string | null,
      targetHistory: string[],
      confessionMessages: [
        {
          id: string,
          message: string,
          explanation: string | null,
          timestamp: Date
        },
        ...
      ],
      votes: {
        count: number,
        required: number
      }
    }
  }
  */
});
```

---

### 2. 플레이어 목록 업데이트
**이벤트**: `player-list-updated`

```javascript
socket.on('player-list-updated', (data) => {
  console.log(data);
  /*
  {
    players: [
      { id: string, name: string },
      ...
    ]
  }
  */
});
```

**발생 시점**: 플레이어가 참가하거나 나갈 때

---

### 3. 게임 시작
**이벤트**: `game-started`

```javascript
socket.on('game-started', (data) => {
  console.log(data);
  /*
  {
    target: string,      // 대상자 ID
    targetName: string   // 대상자 이름
  }
  */
});
```

---

### 4. 채팅 메시지 수신
**이벤트**: `chat-message`

**설명**: 대상자를 제외한 플레이어들만 수신

```javascript
socket.on('chat-message', (data) => {
  console.log(data);
  /*
  {
    senderId: string,
    senderName: string,
    message: string,
    timestamp: Date
  }
  */
});
```

---

### 5. 고해성사 메시지 수신
**이벤트**: `confession-received`

**설명**: 대상자만 수신 (익명)

```javascript
socket.on('confession-received', (data) => {
  console.log(data);
  /*
  {
    id: string,         // 메시지 ID (해명 시 필요)
    message: string,
    timestamp: Date
  }
  */
});
```

---

### 6. 고해성사 메시지 전송 완료
**이벤트**: `confession-sent`

**설명**: 메시지를 보낸 사람만 수신

```javascript
socket.on('confession-sent', (data) => {
  console.log(data);
  /*
  {
    confessionId: string
  }
  */
});
```

---

### 7. 해명 수신
**이벤트**: `explanation-received`

**설명**: 모든 플레이어가 수신

```javascript
socket.on('explanation-received', (data) => {
  console.log(data);
  /*
  {
    confessionId: string,
    explanation: string,
    timestamp: Date
  }
  */
});
```

---

### 8. 투표 현황 업데이트
**이벤트**: `vote-updated`

```javascript
socket.on('vote-updated', (data) => {
  console.log(data);
  /*
  {
    votes: number,      // 현재 투표 수
    required: number    // 필요한 투표 수
  }
  */
});
```

---

### 9. 투표 완료
**이벤트**: `vote-complete`

```javascript
socket.on('vote-complete', (data) => {
  console.log(data);
  /*
  {
    allAgree: boolean  // 모두 동의했는지 여부
  }
  */
});
```

**주의**: `allAgree`가 `true`일 때만 현재 대상자가 다음 대상을 선택할 수 있음

---

### 10. 새로운 대상자 선택됨
**이벤트**: `new-target-selected`

```javascript
socket.on('new-target-selected', (data) => {
  console.log(data);
  /*
  {
    target: string,      // 새 대상자 ID
    targetName: string   // 새 대상자 이름
  }
  */
});
```

---

### 11. 게임 리셋
**이벤트**: `game-reset`

```javascript
socket.on('game-reset', (data) => {
  console.log(data);
  /*
  {
    message: string  // 리셋 사유
  }
  */
});
```

**발생 시점**: 대상자가 방을 나갔을 때

---

### 12. 에러
**이벤트**: `error`

```javascript
socket.on('error', (data) => {
  console.error(data);
  /*
  {
    message: string  // 에러 메시지
  }
  */
});
```

---

## 🎮 게임 플로우

```
1. 플레이어들이 방에 참가 (join-room)
   ↓
2. 호스트가 게임 시작 (start-game)
   ↓
3. 랜덤으로 고해성사 대상자 선정 (game-started)
   ↓
4. 대상자 외 플레이어들:
   - 비밀 채팅 가능 (send-chat-message)
   - 대상자에게 익명 고해성사 메시지 전송 (send-confession)
   ↓
5. 대상자:
   - 받은 메시지에 해명 작성 (send-explanation)
   ↓
6. 대상자 외 플레이어들이 투표 (vote)
   ↓
7. 모두 동의하면 (vote-complete, allAgree: true)
   ↓
8. 현재 대상자가 다음 대상 선택 (select-next-target)
   ↓
9. 3번으로 돌아가서 반복
```

---

## 💡 React 사용 예시

```javascript
import { useEffect, useState } from 'react';
import { io } from 'socket.io-client';

function App() {
  const [socket, setSocket] = useState(null);
  const [roomId, setRoomId] = useState('');
  const [playerId] = useState(() => crypto.randomUUID());
  const [playerName, setPlayerName] = useState('');
  const [players, setPlayers] = useState([]);
  const [currentTarget, setCurrentTarget] = useState(null);
  const [gameState, setGameState] = useState('waiting');
  const [chatMessages, setChatMessages] = useState([]);
  const [confessions, setConfessions] = useState([]);

  useEffect(() => {
    const newSocket = io('http://localhost:3000');
    setSocket(newSocket);

    // 이벤트 리스너 등록
    newSocket.on('join-room-success', (data) => {
      setPlayers(data.room.players);
      setGameState(data.room.gameState);
      setCurrentTarget(data.room.currentTarget);
    });

    newSocket.on('player-list-updated', (data) => {
      setPlayers(data.players);
    });

    newSocket.on('game-started', (data) => {
      setGameState('playing');
      setCurrentTarget(data.target);
    });

    newSocket.on('chat-message', (data) => {
      setChatMessages(prev => [...prev, data]);
    });

    newSocket.on('confession-received', (data) => {
      setConfessions(prev => [...prev, data]);
    });

    newSocket.on('explanation-received', (data) => {
      setConfessions(prev => prev.map(c =>
        c.id === data.confessionId
          ? { ...c, explanation: data.explanation }
          : c
      ));
    });

    newSocket.on('new-target-selected', (data) => {
      setCurrentTarget(data.target);
      setConfessions([]);
    });

    newSocket.on('error', (data) => {
      alert(data.message);
    });

    return () => newSocket.close();
  }, []);

  const joinRoom = () => {
    socket.emit('join-room', {
      roomId,
      playerId,
      playerName
    });
  };

  const startGame = () => {
    socket.emit('start-game', { roomId });
  };

  const sendChatMessage = (message) => {
    socket.emit('send-chat-message', { roomId, message });
  };

  const sendConfession = (message) => {
    socket.emit('send-confession', { roomId, message });
  };

  const sendExplanation = (confessionId, explanation) => {
    socket.emit('send-explanation', { roomId, confessionId, explanation });
  };

  const vote = (agree) => {
    socket.emit('vote', { roomId, agree });
  };

  const selectNextTarget = (targetId) => {
    socket.emit('select-next-target', { roomId, targetId });
  };

  // UI 렌더링...
  return (
    <div>
      {/* React 컴포넌트 구현 */}
    </div>
  );
}

export default App;
```

---

## 🔒 보안 및 제약사항

1. **익명성 보장**: 고해성사 메시지의 발신자는 대상자에게 공개되지 않음
2. **중복 대상 방지**: 한 번 대상이 된 플레이어는 다시 선택될 수 없음 (모든 플레이어가 대상이 되면 히스토리 초기화)
3. **권한 제어**:
   - 대상자는 일반 채팅 수신 불가
   - 대상자는 고해성사 메시지 송신 불가
   - 대상자는 투표 불가
   - 대상자만 해명 가능
   - 현재 대상자만 다음 대상 선택 가능

---

## 🗄️ 데이터 저장

이 게임은 **DB를 사용하지 않으며**, 모든 데이터는 **서버 메모리**에 저장됩니다.
- 서버 재시작 시 모든 방과 게임 상태가 초기화됩니다
- 방에 플레이어가 없으면 자동으로 삭제됩니다