import { useState, useEffect, useCallback } from 'react';
import socket from './socket';
import JoinRoom from './components/JoinRoom';
import WaitingRoom from './components/WaitingRoom';
import GameRoom from './components/GameRoom';
import './App.css';

function App() {
  const [gameState, setGameState] = useState('join'); // 'join' | 'waiting' | 'playing'
  const [roomId, setRoomId] = useState('');
  const [playerId, setPlayerId] = useState('');
  const [playerName, setPlayerName] = useState('');
  const [players, setPlayers] = useState([]);
  const [currentTarget, setCurrentTarget] = useState(null);
  const [confessions, setConfessions] = useState([]);
  const [chatMessages, setChatMessages] = useState([]);
  const [votes, setVotes] = useState({ count: 0, required: 0 });
  const [canSelectNext, setCanSelectNext] = useState(false);
  const [error, setError] = useState('');

  // 소켓 이벤트 핸들러 설정
  useEffect(() => {
    // 방 참가 성공
    socket.on('join-room-success', (data) => {
      console.log('방 참가 성공:', data);
      setPlayers(data.room.players);
      setConfessions(data.room.confessionMessages || []);
      
      if (data.room.gameState === 'playing') {
        setGameState('playing');
        setCurrentTarget(data.room.currentTarget);
        setVotes(data.room.votes || { count: 0, required: 0 });
      } else {
        setGameState('waiting');
      }
    });

    // 플레이어 목록 업데이트
    socket.on('player-list-updated', (data) => {
      console.log('플레이어 목록 업데이트:', data);
      setPlayers(data.players);
    });

    // 게임 시작
    socket.on('game-started', (data) => {
      console.log('게임 시작:', data);
      setGameState('playing');
      setCurrentTarget(data.target);
      setConfessions([]);
      setChatMessages([]);
      setVotes({ count: 0, required: players.length - 1 });
      setCanSelectNext(false);
    });

    // 채팅 메시지 수신
    socket.on('chat-message', (data) => {
      console.log('채팅 메시지:', data);
      setChatMessages(prev => [...prev, data]);
    });

    // 고해성사 메시지 수신 (대상자)
    socket.on('confession-received', (data) => {
      console.log('고해성사 메시지 수신:', data);
      setConfessions(prev => [...prev, { 
        id: data.id, 
        message: data.message, 
        explanation: null,
        timestamp: data.timestamp 
      }]);
    });

    // 고해성사 전송 완료 (발신자)
    socket.on('confession-sent', (data) => {
      console.log('고해성사 전송 완료:', data);
    });

    // 해명 수신
    socket.on('explanation-received', (data) => {
      console.log('해명 수신:', data);
      setConfessions(prev => prev.map(conf => 
        conf.id === data.confessionId 
          ? { ...conf, explanation: data.explanation }
          : conf
      ));
    });

    // 투표 현황 업데이트
    socket.on('vote-updated', (data) => {
      console.log('투표 현황:', data);
      setVotes({ count: data.votes, required: data.required });
    });

    // 투표 완료
    socket.on('vote-complete', (data) => {
      console.log('투표 완료:', data);
      if (data.allAgree) {
        setCanSelectNext(true);
      }
    });

    // 새 대상자 선택됨
    socket.on('new-target-selected', (data) => {
      console.log('새 대상자:', data);
      setCurrentTarget(data.target);
      setConfessions([]);
      setChatMessages([]);
      setVotes({ count: 0, required: players.length - 1 });
      setCanSelectNext(false);
    });

    // 게임 리셋
    socket.on('game-reset', (data) => {
      console.log('게임 리셋:', data);
      setGameState('waiting');
      setCurrentTarget(null);
      setConfessions([]);
      setChatMessages([]);
      setVotes({ count: 0, required: 0 });
      setCanSelectNext(false);
      alert(data.message);
    });

    // 에러
    socket.on('error', (data) => {
      console.error('에러:', data);
      setError(data.message);
      setTimeout(() => setError(''), 3000);
    });

    // 연결 끊김
    socket.on('disconnect', () => {
      console.log('연결 끊김');
    });

    return () => {
      socket.off('join-room-success');
      socket.off('player-list-updated');
      socket.off('game-started');
      socket.off('chat-message');
      socket.off('confession-received');
      socket.off('confession-sent');
      socket.off('explanation-received');
      socket.off('vote-updated');
      socket.off('vote-complete');
      socket.off('new-target-selected');
      socket.off('game-reset');
      socket.off('error');
      socket.off('disconnect');
    };
  }, [players.length]);

  // 방 참가
  const handleJoinRoom = useCallback((data) => {
    setRoomId(data.roomId);
    setPlayerId(data.playerId);
    setPlayerName(data.playerName);
    
    socket.connect();
    socket.emit('join-room', data);
  }, []);

  // 게임 시작
  const handleStartGame = useCallback(() => {
    socket.emit('start-game', { roomId });
  }, [roomId]);

  // 방 나가기
  const handleLeaveRoom = useCallback(() => {
    socket.emit('leave-room', { roomId });
    socket.disconnect();
    setGameState('join');
    setRoomId('');
    setPlayers([]);
    setCurrentTarget(null);
    setConfessions([]);
    setChatMessages([]);
    setVotes({ count: 0, required: 0 });
    setCanSelectNext(false);
  }, [roomId]);

  // 고해성사 전송
  const handleSendConfession = useCallback((message) => {
    socket.emit('send-confession', { roomId, message });
  }, [roomId]);

  // 해명 전송
  const handleSendExplanation = useCallback((confessionId, explanation) => {
    socket.emit('send-explanation', { roomId, confessionId, explanation });
  }, [roomId]);

  // 채팅 전송
  const handleSendChat = useCallback((message) => {
    socket.emit('send-chat-message', { roomId, message });
  }, [roomId]);

  // 투표
  const handleVote = useCallback((agree) => {
    socket.emit('vote', { roomId, agree });
  }, [roomId]);

  // 다음 대상 선택
  const handleSelectNextTarget = useCallback((targetId) => {
    socket.emit('select-next-target', { roomId, targetId });
  }, [roomId]);

  return (
    <div className="app">
      {error && <div className="error-toast">{error}</div>}
      
      {gameState === 'join' && (
        <JoinRoom onJoin={handleJoinRoom} />
      )}
      
      {gameState === 'waiting' && (
        <WaitingRoom
          roomId={roomId}
          players={players}
          currentPlayerId={playerId}
          onStartGame={handleStartGame}
          onLeaveRoom={handleLeaveRoom}
        />
      )}
      
      {gameState === 'playing' && (
        <GameRoom
          roomId={roomId}
          players={players}
          currentPlayerId={playerId}
          currentTarget={currentTarget}
          confessions={confessions}
          votes={votes}
          canSelectNext={canSelectNext}
          onSendConfession={handleSendConfession}
          onSendExplanation={handleSendExplanation}
          onSendChat={handleSendChat}
          onVote={handleVote}
          onSelectNextTarget={handleSelectNextTarget}
          onLeaveRoom={handleLeaveRoom}
          chatMessages={chatMessages}
        />
      )}
    </div>
  );
}

export default App;
