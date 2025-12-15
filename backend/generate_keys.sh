#!/bin/bash

set -euo pipefail

# ---------------------------------------------
# Script to generate RSA 4096-bit PEM key pairs
# and a single-line datasource secret key
# ---------------------------------------------

MAIN_RESOURCES="src/main/resources/keys"
TEST_RESOURCES="src/test/resources/keys"
MAIN_PASS="src/main/resources/pass"
TEST_PASS="src/test/resources/pass"

ACCESS_NAME="access"
REFRESH_NAME="refresh"

mkdir -p "$MAIN_RESOURCES" "$TEST_RESOURCES" "$MAIN_PASS" "$TEST_PASS"

echo "Generating RSA 4096 key pairs..."

create_pem_pair() {
  local prefix=$1
  local out_dir=$2

  local priv_pem="${out_dir}/${prefix}_private.pem"
  local pub_pem="${out_dir}/${prefix}_public.pem"

  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out "$priv_pem"
  openssl rsa -in "$priv_pem" -pubout -out "$pub_pem"

  echo "  -> Generated $priv_pem and $pub_pem"
}

# -------------------------------------------------
# MAIN RESOURCES (always regenerate)
# -------------------------------------------------
echo "Updating main resources..."
create_pem_pair "$ACCESS_NAME" "$MAIN_RESOURCES"
create_pem_pair "$REFRESH_NAME" "$MAIN_RESOURCES"

# Generate SINGLE-LINE secret (NO newline, NO wrapping)
MAIN_SECRET="$(openssl rand -base64 96 | tr -d '\n+/=' | cut -c1-64)"
printf "%s" "$MAIN_SECRET" > "$MAIN_PASS/secret.key"

echo "  -> Main secret saved to $MAIN_PASS/secret.key"

# -------------------------------------------------
# TEST RESOURCES (only if missing)
# -------------------------------------------------
echo "Checking test resources..."

if [[ ! -f "$TEST_RESOURCES/${ACCESS_NAME}_private.pem" || ! -f "$TEST_RESOURCES/${ACCESS_NAME}_public.pem" ]]; then
  create_pem_pair "$ACCESS_NAME" "$TEST_RESOURCES"
else
  echo "Test access keys already exist; skipping."
fi

if [[ ! -f "$TEST_RESOURCES/${REFRESH_NAME}_private.pem" || ! -f "$TEST_RESOURCES/${REFRESH_NAME}_public.pem" ]]; then
  create_pem_pair "$REFRESH_NAME" "$TEST_RESOURCES"
else
  echo "Test refresh keys already exist; skipping."
fi

if [[ ! -f "$TEST_PASS/secret.key" ]]; then
  TEST_SECRET="$(openssl rand -base64 96 | tr -d '\n+/=' | cut -c1-64)"
  printf "%s" "$TEST_SECRET" > "$TEST_PASS/secret.key"
  echo "  -> Test secret saved to $TEST_PASS/secret.key"
else
  echo "Test secret already exists; skipping."
fi

echo "Done!"
