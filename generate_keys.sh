#!/bin/bash
set -euo pipefail

# -----------------------------------------------------------------------------
# Generate RSA 4096-bit keys for authentication resources.
# 
# Usage:
#   ./generate_keys.sh -L <location>
#
# Example:
#   ./generate_keys.sh -L secrets/
# -----------------------------------------------------------------------------

LOCATION=""

# Parse flags
while getopts "L:" opt; do
  case "$opt" in
    L) LOCATION="$OPTARG" ;;
    *) 
      echo "Usage: $0 -L <location>"
      exit 1 
      ;;
  esac
done

# Validation
if [ -z "$LOCATION" ]; then
  echo "Error: The -L flag is required to specify the output location."
  echo "Usage: $0 -L <location>"
  exit 1
fi

# Create directory if it doesn't exist
mkdir -p "$LOCATION"

ACCESS_NAME="access"
REFRESH_NAME="refresh"

create_pem_pair() {
  local prefix=$1
  local out_dir=$2

  local priv_pem="${out_dir}/${prefix}_private.pem"
  local pub_pem="${out_dir}/${prefix}_public.pem"

  echo "Generating RSA 4096 key pair for $prefix..."
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out "$priv_pem"
  openssl rsa -in "$priv_pem" -pubout -out "$pub_pem"

  echo "  -> $priv_pem"
  echo "  -> $pub_pem"
}

echo "Generating keys in: $LOCATION"
echo "----------------------------------------------------------------------------"

# Generate the four requested PEM keys
create_pem_pair "$ACCESS_NAME" "$LOCATION"
create_pem_pair "$REFRESH_NAME" "$LOCATION"

echo "----------------------------------------------------------------------------"
echo "All keys generated successfully in $LOCATION"
