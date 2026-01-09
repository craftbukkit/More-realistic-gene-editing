# Threat Model for QuantaMeet PQ

This document outlines the threat model for QuantaMeet PQ and the mitigations in place.

## 1. Threats

- **Passive Eavesdropping:** An attacker records encrypted traffic to decrypt it later.
- **Active MITM Attack:** An attacker attempts to intercept and modify traffic.
- **Compromised Server:** The server infrastructure (SFU, signaling, database) is compromised.

## 2. Mitigations

- **End-to-End Encryption:** All media and data are encrypted end-to-end, making passive eavesdropping ineffective.
- **PQC-Hybrid Key Exchange:** The use of post-quantum cryptography protects against "harvest now, decrypt later" attacks.
- **Server Unawareness:** The server is designed to be untrusted and has no access to plaintext media or data.
