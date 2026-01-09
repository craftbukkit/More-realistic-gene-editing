# Verification Guide

This guide provides steps to verify the end-to-end encryption (E2EE) and post-quantum cryptography (PQC) in QuantaMeet PQ.

## 1. Verifying Media E2EE

- **Wireshark:** Use Wireshark to inspect SRTP packets. The payload should be encrypted and unreadable.
- **WebRTC Internals:** Use `chrome://webrtc-internals` to verify that SFrame encryption is active.

## 2. Verifying Data E2EE

- **Browser DevTools:** Inspect the WebSocket messages in the Network tab. The payload of DataChannel messages should be encrypted.

## 3. Verifying PQC Key Exchange

- **Nginx Logs:** The Nginx access logs can be configured to log the negotiated cipher suite and TLS group. Look for `X25519Kyber768` to confirm the PQC-hybrid key exchange was used.
- **Wireshark:** A more advanced analysis of the TLS handshake in Wireshark can reveal the exact key exchange algorithm used.
