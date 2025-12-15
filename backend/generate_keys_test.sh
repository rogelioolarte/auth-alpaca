#!/bin/bash
set -euo pipefail

# ---------------------------------------------
# Generate RSA 4096-bit keys for TEST resources
# Only generated if missing
# ---------------------------------------------

TEST_RESOURCES="src/test/resources/keys"
TEST_PASS="src/test/resources/pass"

ACCESS_NAME="access"
REFRESH_NAME="refresh"

mkdir -p "$TEST_RESOURCES" "$TEST_PASS"

echo "Checking TEST RSA keys..."

create_pem_pair() {
  local prefix=$1
  local out_dir=$2

  local priv_pem="${out_dir}/${prefix}_private.pem"
  local pub_pem="${out_dir}/${prefix}_public.pem"

  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out "$priv_pem"
  openssl rsa -in "$priv_pem" -pubout -out "$pub_pem"

  echo "Generated $priv_pem"
  echo "Generated $pub_pem"
}

# ---- Access keys ----
if [[ ! -f "$TEST_RESOURCES/${ACCESS_NAME}_private.pem" || ! -f "$TEST_RESOURCES/${ACCESS_NAME}_public.pem" ]]; then
  create_pem_pair "$ACCESS_NAME" "$TEST_RESOURCES"
else
  echo "Access keys already exist, skipping"
fi

# ---- Refresh keys ----
if [[ ! -f "$TEST_RESOURCES/${REFRESH_NAME}_private.pem" || ! -f "$TEST_RESOURCES/${REFRESH_NAME}_public.pem" ]]; then
  create_pem_pair "$REFRESH_NAME" "$TEST_RESOURCES"
else
  echo "Refresh keys already exist, skipping"
fi

# ---- Secret key ----
if [[ ! -f "$TEST_PASS/secret.key" ]]; then
  TEST_SECRET="$(openssl rand -base64 96 | tr -d '\n+/=' | cut -c1-64)"
  printf "%s" "$TEST_SECRET" > "$TEST_PASS/secret.key"
  echo "Test secret saved to $TEST_PASS/secret.key"
else
  echo "Test secret already exists, skipping"
fi

echo "TEST keys ready"
