#!/bin/bash

# bootstrap-linux.sh: Sets up the environment for QuantaMeet PQ on any mainstream Linux distribution.

echo "Detecting package manager..."

if command -v apt-get &> /dev/null; then
    echo "Debian-based system detected."
    apt-get update
    apt-get install -y docker.io docker-compose openssl curl git
elif command -v dnf &> /dev/null; then
    echo "Red Hat-based system detected."
    dnf install -y docker docker-compose openssl curl git
elif command -v yum &> /dev/null; then
    echo "Older Red Hat-based system detected."
    yum install -y docker docker-compose openssl curl git
elif command -v zypper &> /dev/null; then
    echo "SUSE-based system detected."
    zypper install -y docker docker-compose openssl curl git
elif command -v pacman &> /dev/null; then
    echo "Arch-based system detected."
    pacman -Syu --noconfirm docker docker-compose openssl curl git
else
    echo "Could not detect a supported package manager. Please install Docker, Docker Compose, OpenSSL, curl, and Git manually." >&2
    exit 1
fi

echo "Starting and enabling Docker..."
systemctl start docker
systemctl enable docker

echo "Verifying installations..."
docker --version
docker-compose --version
openssl version
curl --version
git --version

echo "Bootstrap complete."

