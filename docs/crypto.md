# Cryptography Design for QuantaMeet PQ

This document outlines the cryptographic design of QuantaMeet PQ, ensuring true end-to-end encryption (E2EE) with a PQC-first approach.

## 1. WebRTC Security Baseline

- **DTLS-SRTP:** Provides transport-level security for media streams between the client and the SFU. This is a baseline, but it is **not** our E2EE mechanism, as the SFU can still access the media.

## 2. End-to-End Encryption (E2EE)

### 2.1. Media E2EE (SFrame Model)

- **Mechanism:** We use the WebRTC Encoded Transform API to implement an SFrame-like frame-level encryption model.
- **Implementation:** A dedicated Web Worker handles encryption and decryption of encoded video/audio frames, preventing key material from being exposed to the main application thread.

### 2.2. Data E2EE (Chat & Images)

- **Mechanism:** Messages and images sent via WebRTC DataChannels are encrypted using an AEAD cipher.

## 3. PQC/Hybrid Key Establishment

- **Hybrid KEM:** We use a combination of `X25519` and `ML-KEM-768` for key agreement.

## 4. Crypto-agility

- **Suite ID:** All cryptographic operations are versioned with a suite ID.

## 5. Key Lifecycle

- **Key Generation:** Keys are generated on the client-side.
- **Rekeying:** Rekeying is triggered on membership changes (join/leave) and periodically during a session.
