# 🎮 고해성사 게임 - Backend API Server

DB를 사용하지 않는 WebSocket 기반 실시간 익명 고백 게임 서버

## 📋 프로젝트 개요

고해성사 게임은 플레이어들이 실시간으로 익명의 메시지를 주고받으며 진행하는 게임입니다.
모든 데이터는 서버 메모리에 저장되며, DB를 사용하지 않습니다.

### 주요 기능

- ✅ WebSocket (Spring WebSocket) 기반 실시간 통신
- ✅ 방 생성 및 참가
- ✅ 랜덤 고해성사 대상자 선정
- ✅ 대상자를 제외한 플레이어들의 비밀 채팅방
- ✅ 익명 고해성사 메시지 전송
- ✅ 대상자의 해명 기능
- ✅ 투표 시스템 (모든 플레이어 동의 필요)
- ✅ 다음 대상자 선택 (중복 불가)
- ✅ DB 없이 메모리에서 모든 데이터 관리

## 🚀 시작하기

### 필수 요구사항

- Java 17 이상
- Gradle 8.x 이상

### 설치 방법

```bash
# 프로젝트 빌드
./gradlew build

# 서버 실행
./gradlew bootRun

# 또는 JAR 파일로 실행
java -jar build/libs/confession-game-1.0.0.jar
```

### 서버 실행 확인

서버가 정상적으로 실행되면 다음과 같이 접속 가능합니다:

```
WebSocket: ws://localhost:8080/ws
HTTP: http://localhost:8080
```

## 📁 프로젝트 구조

```
UNSEEN/
├── src/
│   ├── main/
│   │   ├── java/com/confession/game/
│   │   │   ├── ConfessionGameApplication.java    # 메인 애플리케이션
│   │   │   ├── domain/                           # 도메인 계층
│   │   │   │   ├── player/
│   │   │   │   │   ├── entity/Player.java
│   │   │   │   │   └── dto/PlayerDto.java
│   │   │   │   ├── room/
│   │   │   │   │   ├── entity/Room.java
│   │   │   │   │   ├── service/RoomService.java
│   │   │   │   │   └── repository/RoomRepository.java
│   │   │   │   ├── confession/
│   │   │   │   │   ├── entity/Confession.java
│   │   │   │   │   └── dto/ConfessionDto.java
│   │   │   │   └── game/
│   │   │   │       └── dto/                       # 게임 관련 DTO
│   │   │   └── global/                            # 글로벌 설정
│   │   │       ├── config/
│   │   │       │   ├── WebSocketConfig.java       # WebSocket 설정
│   │   │       │   └── WebConfig.java             # CORS 설정
│   │   │       ├── handler/
│   │   │       │   └── WebSocketHandler.java      # WebSocket 핸들러
│   │   │       └── common/
│   │   │           └── BaseResponse.java          # 공통 응답 DTO
│   │   └── resources/
│   │       └── application.yml                    # 설정 파일
│   └── test/
├── build.gradle                                   # 빌드 설정
├── API_DOCUMENTATION.md                           # WebSocket API 상세 명세서
└── README.md                                      # 프로젝트 문서
```

## 🔌 API 사용법

### WebSocket 클라이언트 연결

```javascript
const socket = new WebSocket('ws://localhost:8080/ws');

socket.onopen = () => {
  console.log('WebSocket 연결 성공');
};

socket.onmessage = (event) => {
  const response = JSON.parse(event.data);
  console.log('받은 메시지:', response);
};

// 메시지 전송
function sendMessage(type, data) {
  socket.send(JSON.stringify({ type, data }));
}
```

### 주요 이벤트

**클라이언트 → 서버**
- `join-room`: 방 참가
- `start-game`: 게임 시작
- `send-chat-message`: 일반 채팅 (대상 제외)
- `send-confession`: 고해성사 메시지 전송
- `send-explanation`: 해명 전송 (대상자만)
- `vote`: 투표 (동의/비동의)
- `select-next-target`: 다음 대상자 선택 (현재 대상자만)
- `leave-room`: 방 나가기

**서버 → 클라이언트**
- `join-room-success`: 방 참가 성공
- `player-list-updated`: 플레이어 목록 업데이트
- `game-started`: 게임 시작 알림
- `chat-message`: 채팅 메시지 수신
- `confession-received`: 고해성사 메시지 수신
- `explanation-received`: 해명 수신
- `vote-updated`: 투표 현황
- `vote-complete`: 투표 완료
- `new-target-selected`: 새 대상자 선택됨
- `game-reset`: 게임 리셋
- `error`: 에러 발생

자세한 API 명세는 [`API_DOCUMENTATION.md`](./API_DOCUMENTATION.md)를 참고하세요.

## 🎯 게임 플로우

```
1️⃣ 플레이어들이 방에 참가
   ↓
2️⃣ 게임 시작 (최소 2명)
   ↓
3️⃣ 랜덤으로 고해성사 대상자 선정
   ↓
4️⃣ 대상자 외 플레이어들:
   - 비밀 채팅방에서 대화
   - 대상자에게 익명 고해성사 메시지 전송
   ↓
5️⃣ 대상자:
   - 받은 메시지에 대해 해명 작성
   ↓
6️⃣ 대상자 외 플레이어들 투표
   ↓
7️⃣ 모두 동의하면
   ↓
8️⃣ 현재 대상자가 다음 대상 선택 (중복 불가)
   ↓
9️⃣ 3번으로 돌아가서 반복
```

## 🏗️ 아키텍처

### 클린 아키텍처 적용

**Domain 계층**
- `domain/player`: 플레이어 엔티티 및 DTO
- `domain/room`: 방 엔티티, 서비스, 레포지토리
- `domain/confession`: 고해성사 메시지 엔티티 및 DTO
- `domain/game`: 게임 관련 DTO

**Global 계층**
- `global/config`: WebSocket 및 CORS 설정
- `global/handler`: WebSocket 이벤트 핸들러
- `global/common`: 공통 응답 DTO

### 핵심 컴포넌트

**RoomRepository**
- 여러 방(Room)을 관리
- ConcurrentHashMap을 사용한 메모리 기반 데이터 저장
- Thread-safe한 동시성 처리

**RoomService**
- 비즈니스 로직 처리
- 방 생성, 참가, 게임 시작, 투표 등 핵심 기능 제공

**WebSocketHandler**
- 실시간 메시지 처리
- 세션 관리 및 방별 브로드캐스팅
- 이벤트 기반 통신 처리

## 🔐 보안 및 제약사항

### 익명성 보장
- 고해성사 메시지의 발신자는 대상자에게 공개되지 않음
- 서버에서만 발신자 정보 관리 (중복 방지용)

### 권한 제어
- **대상자는**:
  - ❌ 일반 채팅 수신 불가
  - ❌ 고해성사 메시지 송신 불가
  - ❌ 투표 불가
  - ✅ 해명만 작성 가능
  - ✅ 다음 대상자 선택 가능 (모두 동의 시)

- **일반 플레이어는**:
  - ✅ 비밀 채팅 가능
  - ✅ 고해성사 메시지 송신 가능
  - ✅ 투표 가능

### 중복 방지
- 한 번 대상이 된 플레이어는 다시 선택될 수 없음
- 모든 플레이어가 대상이 되면 히스토리 초기화

## 💾 데이터 저장

이 프로젝트는 **DB를 사용하지 않습니다**.

- 모든 데이터는 **서버 메모리**에 저장됩니다
- 서버 재시작 시 모든 방과 게임 상태가 초기화됩니다
- 방에 플레이어가 없으면 자동으로 삭제됩니다

## 🛠️ 기술 스택

- **Java 17**: 프로그래밍 언어
- **Spring Boot 4.0.1**: 애플리케이션 프레임워크
- **Spring WebSocket**: 실시간 양방향 통신
- **Lombok**: 보일러플레이트 코드 제거
- **Jackson**: JSON 직렬화/역직렬화
- **Gradle**: 빌드 도구

## 📝 React와 함께 사용하기

```javascript
import { useEffect, useState, useRef } from 'react';

function App() {
  const [playerId] = useState(() => crypto.randomUUID());
  const socketRef = useRef(null);

  useEffect(() => {
    const socket = new WebSocket('ws://localhost:8080/ws');
    socketRef.current = socket;

    socket.onopen = () => {
      console.log('WebSocket 연결 성공');
    };

    socket.onmessage = (event) => {
      const response = JSON.parse(event.data);

      switch (response.type) {
        case 'game-started':
          console.log('게임 시작!', response.data);
          break;
        case 'confession-received':
          console.log('고해성사 메시지:', response.data);
          break;
        case 'error':
          console.error('에러:', response.data.message);
          break;
        // ... 기타 이벤트 처리
      }
    };

    socket.onclose = () => {
      console.log('WebSocket 연결 종료');
    };

    return () => socket.close();
  }, []);

  const sendMessage = (type, data) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify({ type, data }));
    }
  };

  const joinRoom = (roomId, playerName) => {
    sendMessage('join-room', { roomId, playerId, playerName });
  };

  const startGame = (roomId) => {
    sendMessage('start-game', { roomId });
  };

  const sendConfession = (roomId, message) => {
    sendMessage('send-confession', { roomId, message });
  };

  return <div>{/* UI 구현 */}</div>;
}
```

더 자세한 API 명세는 [`API_DOCUMENTATION.md`](./API_DOCUMENTATION.md)를 참고하세요.

## 🐛 디버깅

서버는 콘솔에 다음과 같은 로그를 출력합니다:

```
클라이언트 연결됨: <socket-id>
플레이어 <name> (<id>)가 방 <room-id>에 참가했습니다.
방 <room-id>에서 게임 시작. 대상: <target-id>
채팅 메시지: <name>: <message>
고해성사 메시지 전송: <message>
해명 전송: <explanation>
투표 완료. 모두 동의: <true/false>
새로운 대상 선택됨: <target-id>
플레이어가 방 <room-id>을 나갔습니다.
클라이언트 연결 해제: <socket-id>
```

## 📄 라이선스

MIT

## 👨‍💻 개발자

고해성사 게임 백엔드 API

---

**문의사항이나 버그 리포트는 이슈로 등록해주세요!**