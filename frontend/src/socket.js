const SOCKET_URL = 'ws://localhost:8080/ws';

class SocketClient {
  constructor() {
    this.socket = null;
    this.listeners = new Map();
    this.isConnected = false;
  }

  connect() {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return;
    }

    this.socket = new WebSocket(SOCKET_URL);

    this.socket.onopen = () => {
      console.log('WebSocket 연결됨');
      this.isConnected = true;
      this.emit('connect');
    };

    this.socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const type = data.type;
        const payload = data.data;
        console.log('받은 메시지:', type, payload);
        
        const callbacks = this.listeners.get(type) || [];
        callbacks.forEach(callback => callback(payload));
      } catch (e) {
        console.error('메시지 파싱 오류:', e);
      }
    };

    this.socket.onclose = () => {
      console.log('WebSocket 연결 끊김');
      this.isConnected = false;
      this.emit('disconnect');
    };

    this.socket.onerror = (error) => {
      console.error('WebSocket 오류:', error);
    };
  }

  disconnect() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
      this.isConnected = false;
    }
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event).push(callback);
  }

  off(event, callback) {
    if (!this.listeners.has(event)) return;
    
    if (callback) {
      const callbacks = this.listeners.get(event);
      const index = callbacks.indexOf(callback);
      if (index > -1) {
        callbacks.splice(index, 1);
      }
    } else {
      this.listeners.delete(event);
    }
  }

  emit(type, data) {
    if (type === 'connect' || type === 'disconnect') {
      const callbacks = this.listeners.get(type) || [];
      callbacks.forEach(callback => callback());
      return;
    }

    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({ type, data });
      console.log('보내는 메시지:', type, data);
      this.socket.send(message);
    } else {
      console.error('WebSocket이 연결되지 않았습니다');
    }
  }
}

const socket = new SocketClient();
export default socket;
