#!/bin/bash

# Check if the .env file exists
if [ ! -f .env ]; then
  echo "The .env file does not exist in the current directory."
  exit 1
fi

# Export variables from the .env file ensuring UTF-8 compatibility
while IFS='=' read -r key value; do
  # Ignore empty lines and comments
  if [[ -n "$key" && ! "$key" =~ ^# ]]; then
    # Remove leading and trailing spaces from the key and value
    key=$(echo "$key" | xargs)
    value=$(echo "$value" | xargs)

    # Remove any surrounding quotes from value
    value=$(echo "$value" | sed 's/^["'\''\(]//;s/["'\''\)]$//')

    # Export the key-value pair
    export "$key=$value"
  fi
done < .env

./mvnw spring-boot:run
