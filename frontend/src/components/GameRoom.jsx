import { useState, useEffect, useRef } from 'react';
import './GameRoom.css';

function GameRoom({ 
  roomId, 
  players, 
  currentPlayerId, 
  currentTarget,
  confessions,
  votes,
  canSelectNext,
  onSendConfession,
  onSendExplanation,
  onSendChat,
  onVote,
  onSelectNextTarget,
  onLeaveRoom,
  chatMessages
}) {
  const [confessionInput, setConfessionInput] = useState('');
  const [explanationInput, setExplanationInput] = useState('');
  const [chatInput, setChatInput] = useState('');
  const [selectedConfessionId, setSelectedConfessionId] = useState(null);
  const [hasVoted, setHasVoted] = useState(false);
  
  const confessionsEndRef = useRef(null);
  const chatEndRef = useRef(null);

  const isTarget = currentTarget === currentPlayerId;
  const targetPlayer = players.find(p => p.id === currentTarget);

  useEffect(() => {
    confessionsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [confessions]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages]);

  useEffect(() => {
    setHasVoted(false);
  }, [currentTarget]);

  const handleSendConfession = (e) => {
    e.preventDefault();
    if (!confessionInput.trim() || isTarget) return;
    onSendConfession(confessionInput.trim());
    setConfessionInput('');
  };

  const handleSendExplanation = (e) => {
    e.preventDefault();
    if (!explanationInput.trim() || !selectedConfessionId) return;
    onSendExplanation(selectedConfessionId, explanationInput.trim());
    setExplanationInput('');
    setSelectedConfessionId(null);
  };

  const handleSendChat = (e) => {
    e.preventDefault();
    if (!chatInput.trim() || isTarget) return;
    onSendChat(chatInput.trim());
    setChatInput('');
  };

  const handleVote = (agree) => {
    if (isTarget || hasVoted) return;
    onVote(agree);
    setHasVoted(true);
  };

  const availableTargets = players.filter(p => p.id !== currentPlayerId);

  return (
    <div className="game-room-container">
      {/* 헤더 */}
      <header className="game-header">
        <div className="header-left">
          <span className="room-badge">방: {roomId}</span>
          <span className="player-count">{players.length}명 참가중</span>
        </div>
        <button className="leave-btn" onClick={onLeaveRoom}>나가기</button>
      </header>

      <div className="game-content">
        {/* 고해 목록 (피해자 오임) */}
        <div className={`card confessions-card ${isTarget ? 'target-view' : ''}`}>
          <div className="card-header cyan">
            <h3>{isTarget ? '🎯 나에게 온 고해' : '📝 피해자 오임'}</h3>
          </div>
          <div className="card-body">
            <div className="target-info">
              현재 대상: <strong>{targetPlayer?.name || '없음'}</strong>
              {isTarget && <span className="target-badge">👆 당신입니다!</span>}
            </div>
            
            <div className="confessions-list">
              {confessions.length === 0 ? (
                <p className="empty-message">아직 고해성사가 없습니다</p>
              ) : (
                confessions.map((confession) => (
                  <div key={confession.id} className="confession-item">
                    <div className="confession-message">
                      <span className="anon-badge">익명</span>
                      {confession.message}
                    </div>
                    {confession.explanation && (
                      <div className="explanation">
                        <span className="explanation-badge">해명</span>
                        {confession.explanation}
                      </div>
                    )}
                    {isTarget && !confession.explanation && (
                      <button 
                        className="explain-btn"
                        onClick={() => setSelectedConfessionId(confession.id)}
                      >
                        해명하기
                      </button>
                    )}
                  </div>
                ))
              )}
              <div ref={confessionsEndRef} />
            </div>

            {/* 해명 입력 (대상자용) */}
            {isTarget && selectedConfessionId && (
              <form onSubmit={handleSendExplanation} className="input-form">
                <input
                  type="text"
                  value={explanationInput}
                  onChange={(e) => setExplanationInput(e.target.value)}
                  placeholder="해명을 입력하세요..."
                />
                <button type="submit">해명</button>
                <button type="button" onClick={() => setSelectedConfessionId(null)}>취소</button>
              </form>
            )}
          </div>
        </div>

        {/* 고발하기 */}
        <div className="card confession-input-card">
          <div className="card-header pink">
            <h3>😈 고발하기</h3>
          </div>
          <div className="card-body">
            <div className="target-display">
              대상자: <strong>{targetPlayer?.name || '없음'}</strong>
            </div>
            
            {!isTarget ? (
              <form onSubmit={handleSendConfession} className="confession-form">
                <textarea
                  value={confessionInput}
                  onChange={(e) => setConfessionInput(e.target.value)}
                  placeholder="메시지를 입력하세요"
                  rows={3}
                />
                <p className="anon-notice">🔒 당신 사유 없음 실효</p>
                <button type="submit" className="send-btn">발송하기</button>
              </form>
            ) : (
              <div className="target-notice">
                <p>🎯 당신이 현재 고해성사 대상입니다!</p>
                <p>다른 플레이어들의 고해를 기다리세요.</p>
              </div>
            )}
          </div>
        </div>

        {/* 벌문 / 투표 / 다음 대상 선택 */}
        <div className="card punishment-card">
          <div className="card-header yellow">
            <h3>⚖️ 벌문</h3>
          </div>
          <div className="card-body">
            {/* 고발 내용 요약 */}
            <div className="punishment-summary">
              <p className="summary-label">고발 내용</p>
              {confessions.length > 0 ? (
                <p className="summary-content">{confessions[confessions.length - 1]?.message}</p>
              ) : (
                <p className="empty-message">아직 고발 내용이 없습니다</p>
              )}
            </div>

            {/* 투표 섹션 */}
            <div className="vote-section">
              <div className="vote-status">
                <span>투표 현황: {votes.count} / {votes.required}</span>
              </div>
              
              {!isTarget && (
                <div className="vote-buttons">
                  <button 
                    className={`vote-btn agree ${hasVoted ? 'disabled' : ''}`}
                    onClick={() => handleVote(true)}
                    disabled={hasVoted}
                  >
                    👍 동의
                  </button>
                  <button 
                    className={`vote-btn disagree ${hasVoted ? 'disabled' : ''}`}
                    onClick={() => handleVote(false)}
                    disabled={hasVoted}
                  >
                    👎 반대
                  </button>
                </div>
              )}
              
              {hasVoted && <p className="voted-notice">자놈이 조 꼬셨어요!!</p>}
            </div>

            {/* 다음 대상 선택 (대상자용) */}
            {isTarget && canSelectNext && (
              <div className="next-target-section">
                <p className="select-label">벌문</p>
                <div className="target-buttons">
                  {availableTargets.map(player => (
                    <button
                      key={player.id}
                      className="target-btn"
                      onClick={() => onSelectNextTarget(player.id)}
                    >
                      {player.name}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 채팅 (대상자 제외) */}
      {!isTarget && (
        <div className="chat-section">
          <div className="chat-header">💬 비밀 채팅 (대상자에게 안 보임)</div>
          <div className="chat-messages">
            {chatMessages.map((msg, idx) => (
              <div key={idx} className="chat-message">
                <span className="chat-sender">{msg.senderName}:</span>
                <span className="chat-content">{msg.message}</span>
              </div>
            ))}
            <div ref={chatEndRef} />
          </div>
          <form onSubmit={handleSendChat} className="chat-form">
            <input
              type="text"
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              placeholder="메시지를 입력하세요..."
            />
            <button type="submit">전송</button>
          </form>
        </div>
      )}
    </div>
  );
}

export default GameRoom;
