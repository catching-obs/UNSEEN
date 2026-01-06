import { useState } from 'react';
import { v4 as uuidv4 } from 'uuid';
import './JoinRoom.css';

function JoinRoom({ onJoin }) {
  const [roomId, setRoomId] = useState('');
  const [playerName, setPlayerName] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (!roomId.trim()) {
      setError('방 ID를 입력해주세요');
      return;
    }
    
    if (!playerName.trim()) {
      setError('닉네임을 입력해주세요');
      return;
    }

    const playerId = localStorage.getItem('playerId') || uuidv4();
    localStorage.setItem('playerId', playerId);
    
    onJoin({
      roomId: roomId.trim(),
      playerId,
      playerName: playerName.trim(),
    });
  };

  return (
    <div className="join-room-container">
      <div className="join-room-card">
        <h1 className="title">🙏 고해성사 게임</h1>
        <p className="subtitle">당신의 죄를 고백하고 벌을 받으세요</p>
        
        <form onSubmit={handleSubmit} className="join-form">
          <div className="input-group">
            <label htmlFor="roomId">방 코드</label>
            <input
              id="roomId"
              type="text"
              value={roomId}
              onChange={(e) => setRoomId(e.target.value)}
              placeholder="방 코드를 입력하세요"
            />
          </div>
          
          <div className="input-group">
            <label htmlFor="playerName">닉네임</label>
            <input
              id="playerName"
              type="text"
              value={playerName}
              onChange={(e) => setPlayerName(e.target.value)}
              placeholder="닉네임을 입력하세요"
              maxLength={20}
            />
          </div>
          
          {error && <p className="error-message">{error}</p>}
          
          <button type="submit" className="join-button">
            입장하기
          </button>
        </form>
      </div>
    </div>
  );
}

export default JoinRoom;
