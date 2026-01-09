import React, { useEffect, useRef } from 'react';
import { Participant, Track } from 'livekit-client';

interface ParticipantProps {
  participant: Participant;
}

export function Participant({ participant }: ParticipantProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const audioRef = useRef<HTMLAudioElement>(null);

  useEffect(() => {
    const onTrackSubscribed = (track: Track) => {
      if (track.kind === Track.Kind.Video) {
        if (videoRef.current) {
          track.attach(videoRef.current);
        }
      } else if (track.kind === Track.Kind.Audio) {
        if (audioRef.current) {
          track.attach(audioRef.current);
        }
      }
    };

    const onTrackUnsubscribed = (track: Track) => {
      if (track.kind === Track.Kind.Video) {
        track.detach();
      } else if (track.kind === Track.Kind.Audio) {
        track.detach();
      }
    };

    participant.on(Track.Subscribed, onTrackSubscribed);
    participant.on(Track.Unsubscribed, onTrackUnsubscribed);

    // Attach existing tracks
    participant.tracks.forEach((publication) => {
      if (publication.track) {
        onTrackSubscribed(publication.track);
      }
    });

    return () => {
      participant.off(Track.Subscribed, onTrackSubscribed);
      participant.off(Track.Unsubscribed, onTrackUnsubscribed);
    };
  }, [participant]);

  return (
    <div className="participant">
      <p>{participant.identity}</p>
      <video ref={videoRef} autoPlay muted={participant.isLocal}></video>
      <audio ref={audioRef} autoPlay></audio>
    </div>
  );
}
