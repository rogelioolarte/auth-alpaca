#!/bin/bash

# ---------------------------------------------
# Script to generate RSA 4096-bit PEM key pairs
# for JWT access and refresh tokens, and to
# generate a secure database secret key.
#
# - Always regenerate keys in main/resources.
# - Only generate test keys if they do not exist.
# - Generate SPRING_DATASOURCE_SECRET_KEY for both
#   main and test environments.
#
# Requires: OpenSSL installed on the system.
# ---------------------------------------------

set -euo pipefail

# Configure resource paths relative to project root
MAIN_RESOURCES="src/main/resources/keys"
TEST_RESOURCES="src/test/resources/keys"
MAIN_PASS="src/main/resources/pass"
TEST_PASS="src/test/resources/pass"

# Base names for the key pairs
ACCESS_NAME="access"
REFRESH_NAME="refresh"

# Create directories for secret key storage if missing
mkdir -p "$MAIN_RESOURCES"
mkdir -p "$TEST_RESOURCES"
mkdir -p "$MAIN_PASS"
mkdir -p "$TEST_PASS"

echo "Generating RSA 4096 key pairs..."

# Function that generates a PEM key pair for a given prefix in a given directory.
# Uses OpenSSL to create a 4096-bit private key and then extract the matching public key.
function create_pem_pair() {
  local prefix=$1
  local out_dir=$2

  local priv_pem="${out_dir}/${prefix}_private.pem"
  local pub_pem="${out_dir}/${prefix}_public.pem"

  # Generate a new RSA private key (PKCS#8)
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out "$priv_pem"

  # Extract the corresponding public key (X.509 format)
  openssl rsa -in "$priv_pem" -pubout -out "$pub_pem"

  echo "  -> Generated $priv_pem and $pub_pem"
}

# --- main/resources: always create or replace keys ---
echo "Updating main resources..."
create_pem_pair "$ACCESS_NAME" "$MAIN_RESOURCES"
create_pem_pair "$REFRESH_NAME" "$MAIN_RESOURCES"

# Generate a strong random secret for datasource encryption
MAIN_SECRET=$(openssl rand -base64 64 | tr -d '+/=' | head -c 64)
echo "SPRING_DATASOURCE_SECRET_KEY=${MAIN_SECRET}" > "$MAIN_PASS/secret.key"
echo "  -> Main secret saved to $MAIN_PASS/secret.key"

# --- test/resources: generate only if missing ---
echo "Checking test resources..."

if [ ! -f "$TEST_RESOURCES/${ACCESS_NAME}_private.pem" ] || [ ! -f "$TEST_RESOURCES/${ACCESS_NAME}_public.pem" ]; then
  echo "Generating test access key pair..."
  create_pem_pair "$ACCESS_NAME" "$TEST_RESOURCES"
else
  echo "Test access keys already exist; skipping overwrite."
fi

if [ ! -f "$TEST_RESOURCES/${REFRESH_NAME}_private.pem" ] || [ ! -f "$TEST_RESOURCES/${REFRESH_NAME}_public.pem" ]; then
  echo "Generating test refresh key pair..."
  create_pem_pair "$REFRESH_NAME" "$TEST_RESOURCES"
else
  echo "Test refresh keys already exist; skipping overwrite."
fi

# Only create the test secret if it does not already exist
if [ ! -f "$TEST_PASS/secret.key" ]; then
  TEST_SECRET=$(openssl rand -base64 64 | tr -d '+/=' | head -c 64)
  echo "${TEST_SECRET}" > "$TEST_PASS/secret.key"
  echo "  -> Test secret saved to $TEST_PASS/secret.key"
else
  echo "Test secret already exists; skipping."
fi

echo "Done!"
