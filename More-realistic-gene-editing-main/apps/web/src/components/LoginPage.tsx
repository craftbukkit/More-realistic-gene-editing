import React, { useState } from 'react';

interface LoginPageProps {
  onLogin: (username: string, roomName: string) => void;
}

export function LoginPage({ onLogin }: LoginPageProps) {
  const [username, setUsername] = useState('');
  const [roomName, setRoomName] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (username && roomName) {
      onLogin(username, roomName);
    }
  };

  return (
    <div>
      <h2>Join a Room</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="username">Username:</label>
          <input
            type="text"
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </div>
        <div>
          <label htmlFor="roomName">Room Name:</label>
          <input
            type="text"
            id="roomName"
            value={roomName}
            onChange={(e) => setRoomName(e.target.value)}
          />
        </div>
        <button type="submit">Join</button>
      </form>
    </div>
  );
}
