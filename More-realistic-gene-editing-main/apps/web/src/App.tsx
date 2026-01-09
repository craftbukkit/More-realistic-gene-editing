import React, { useState } from 'react';
import { LoginPage } from './components/LoginPage';
import { RoomPage } from './components/RoomPage';

function App() {
  const [username, setUsername] = useState('');
  const [roomName, setRoomName] = useState('');

  const handleLogin = (user: string, room: string) => {
    setUsername(user);
    setRoomName(room);
  };

  const handleLeave = () => {
    setUsername('');
    setRoomName('');
  };

  return (
    <div>
      <h1>Welcome to QuantaMeet PQ</h1>
      {!username || !roomName ? (
        <LoginPage onLogin={handleLogin} />
      ) : (
        <RoomPage onLeave={handleLeave} username={username} roomName={roomName} />
      )}
    </div>
  );
}

export default App;
