#!/bin/bash
set -euo pipefail

# ---------------------------------------------
# Generate RSA 4096-bit keys for MAIN resources
# Always regenerated
# ---------------------------------------------

MAIN_RESOURCES="src/main/resources/keys"
MAIN_PASS="src/main/resources/pass"

ACCESS_NAME="access"
REFRESH_NAME="refresh"

mkdir -p "$MAIN_RESOURCES" "$MAIN_PASS"

echo "Generating MAIN RSA 4096 key pairs..."

create_pem_pair() {
  local prefix=$1
  local out_dir=$2

  local priv_pem="${out_dir}/${prefix}_private.pem"
  local pub_pem="${out_dir}/${prefix}_public.pem"

  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out "$priv_pem"
  openssl rsa -in "$priv_pem" -pubout -out "$pub_pem"

  echo " $priv_pem"
  echo " $pub_pem"
}

# ---- Keys ----
create_pem_pair "$ACCESS_NAME" "$MAIN_RESOURCES"
create_pem_pair "$REFRESH_NAME" "$MAIN_RESOURCES"

# ---- Secret key (single-line, deterministic length) ----
MAIN_SECRET="$(openssl rand -base64 96 | tr -d '\n+/=' | cut -c1-64)"
printf "%s" "$MAIN_SECRET" > "$MAIN_PASS/secret.key"

echo "Main secret saved to $MAIN_PASS/secret.key"
echo "MAIN keys generated successfully"