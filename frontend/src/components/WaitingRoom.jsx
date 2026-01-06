import './WaitingRoom.css';

function WaitingRoom({ roomId, players, currentPlayerId, onStartGame, onLeaveRoom }) {
  const isHost = players.length > 0 && players[0].id === currentPlayerId;
  const canStart = players.length >= 2;

  return (
    <div className="waiting-room-container">
      <div className="waiting-room-card">
        <div className="room-header">
          <h2>방 코드: <span className="room-code">{roomId}</span></h2>
          <button className="leave-button" onClick={onLeaveRoom}>
            나가기
          </button>
        </div>

        <div className="players-section">
          <h3>참가자 목록 ({players.length}명)</h3>
          <div className="players-list">
            {players.map((player, index) => (
              <div 
                key={player.id} 
                className={`player-item ${player.id === currentPlayerId ? 'me' : ''}`}
              >
                <span className="player-avatar">
                  {index === 0 ? '👑' : '😈'}
                </span>
                <span className="player-name">
                  {player.name}
                  {player.id === currentPlayerId && ' (나)'}
                </span>
              </div>
            ))}
          </div>
        </div>

        {isHost ? (
          <button 
            className={`start-button ${!canStart ? 'disabled' : ''}`}
            onClick={onStartGame}
            disabled={!canStart}
          >
            {canStart ? '게임 시작하기' : '최소 2명이 필요합니다'}
          </button>
        ) : (
          <div className="waiting-message">
            <div className="loader"></div>
            <p>호스트가 게임을 시작하기를 기다리는 중...</p>
          </div>
        )}
      </div>

      <div className="game-rules">
        <h3>🎮 게임 방법</h3>
        <ol>
          <li>게임이 시작되면 한 명이 <strong>고해성사 대상자</strong>로 선정됩니다</li>
          <li>다른 플레이어들은 대상자에게 <strong>익명으로</strong> 고해성사 메시지를 보냅니다</li>
          <li>대상자는 받은 메시지에 대해 <strong>해명</strong>을 할 수 있습니다</li>
          <li>모든 플레이어가 동의하면 대상자가 <strong>다음 대상</strong>을 선택합니다</li>
          <li>재미있는 벌칙을 정해서 함께 즐겨보세요! 🎉</li>
        </ol>
      </div>
    </div>
  );
}

export default WaitingRoom;
