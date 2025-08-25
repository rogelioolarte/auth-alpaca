#!/bin/bash

# Enable "strict mode" to catch errors and prevent failed commands from continuing
set -euo pipefail

# Create temporary files for key storage
PRIVATE_PEM=$(mktemp)
PUBLIC_PEM=$(mktemp)
PRIVATE_PKCS8=$(mktemp)

# Generate a 4096-bit RSA private key in PEM format
openssl genrsa -out "$PRIVATE_PEM" 4096

# Generate the corresponding public key
openssl rsa -in "$PRIVATE_PEM" -out "$PUBLIC_PEM" -pubout

# Convert the private key to PKCS#8 format (without encryption)
openssl pkcs8 -topk8 -inform PEM -in "$PRIVATE_PEM" -outform PEM -out "$PRIVATE_PKCS8" -nocrypt

# Extract and format the keys as environment variables
PRIVATE_KEY=$(sed '1d;$d' "$PRIVATE_PKCS8" | tr -d '\n')
PUBLIC_KEY=$(sed '1d;$d' "$PUBLIC_PEM" | tr -d '\n')

# Print the environment variables
echo "JWT_BE_PRIVATE_KEY=${PRIVATE_KEY}"
echo "JWT_BE_PUBLIC_KEY=${PUBLIC_KEY}"

# Securely remove temporary files
rm -f "$PRIVATE_PEM" "$PUBLIC_PEM" "$PRIVATE_PKCS8"

# Generate a secure HS512 secret key in base64 format without unsafe characters
SECRET_KEY=$(openssl rand -base64 64 | tr -d '+/=' | head -c 64)
echo "SPRING_DATASOURCE_SECRET_KEY=${SECRET_KEY}"
