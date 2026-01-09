import React, { useEffect, useState } from 'react';
import { Room, RoomEvent, LocalParticipant, RemoteParticipant, Participant as LiveKitParticipant } from 'livekit-client';
import { Participant } from './Participant';

interface RoomPageProps {
  onLeave: () => void;
  username: string;
  roomName: string;
}

export function RoomPage({ onLeave, username, roomName }: RoomPageProps) {
  const [room, setRoom] = useState<Room | null>(null);
  const [participants, setParticipants] = useState<LiveKitParticipant[]>([]);

  useEffect(() => {
    const connectToRoom = async () => {
      // 1. Get a token from the auth service
      const authRes = await fetch('/api/auth/token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, roomName }),
      });
      const { token } = await authRes.json();

      // 2. Get the encryption key from the KMS
      const keyRes = await fetch('/api/kms/key');
      const { key } = await keyRes.json();

      // 3. Connect to the LiveKit room
      const newRoom = new Room({
        // The URL of the LiveKit server
        url: 'ws://localhost:7880',
        // The token for the user
        token,
        // E2EE options
        e2ee: {
          keyProvider: () => new TextEncoder().encode(key),
          // Use shared key E2EE
          sharedKey: true,
        },
      });

      // 4. Set up event listeners for the room
      newRoom
        .on(RoomEvent.ParticipantConnected, (participant: RemoteParticipant) => {
          console.log('Participant connected:', participant.identity);
          setParticipants((prev) => [...prev, participant]);
        })
        .on(RoomEvent.ParticipantDisconnected, (participant: RemoteParticipant) => {
          console.log('Participant disconnected:', participant.identity);
          setParticipants((prev) => prev.filter((p) => p.identity !== participant.identity));
        });

      // 5. Connect to the room and publish local tracks
      await newRoom.connect();
      await newRoom.localParticipant.setMicrophoneEnabled(true);
      await newRoom.localParticipant.setCameraEnabled(true);

      // 6. Set the room and initial participants in state
      setRoom(newRoom);
      setParticipants([newRoom.localParticipant, ...newRoom.participants.values()]);
    };

    connectToRoom();

    // 7. Clean up the room when the component unmounts
    return () => {
      room?.disconnect();
    };
  }, [username, roomName]);

  const handleLeave = () => {
    room?.disconnect();
    onLeave();
  };

  return (
    <div>
      <h2>Room: {roomName}</h2>
      <p>Welcome, {username}!</p>
      <button onClick={handleLeave}>Leave Room</button>
      <div className="participants">
        {participants.map((participant) => (
          <Participant key={participant.identity} participant={participant} />
        ))}
      </div>
    </div>
  );
}
