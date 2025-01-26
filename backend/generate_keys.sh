#!/bin/bash

# Generate pem private key in PEM-encoded X.509 format
openssl genrsa -out refresh.pem 4096
# generate public key
openssl rsa -in refresh.pem -out refresh.pub -pubout
# Convert the private key to PKCS#8 format
openssl pkcs8 -topk8 -inform PEM -in refresh.pem -outform PEM -out refresh.key -nocrypt

PRIVATE_KEY=$(sed '1d;$d' refresh.key | tr -d '\n')
PUBLIC_KEY=$(sed '1d;$d' refresh.pub | tr -d '\n')

echo "JWT_BE_PRIVATE_KEY=${PRIVATE_KEY}"
echo "JWT_BE_PUBLIC_KEY=${PUBLIC_KEY}"

rm refresh.pem refresh.key refresh.pub

# Generate a HS512 key in base64 format
SECRET_KEY=$(openssl rand -base64 64 | tr -d '\n')

echo "SPRING_DATASOURCE_SECRET_KEY=${SECRET_KEY}"