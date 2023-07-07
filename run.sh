#!/usr/bin/env bash
cd $(dirname $0)
echo "ABS mod signing and obfuscation is currently deprecated.
Use at your own risk." >> /dev/stderr

env ABS_CUSTOMER_ID=$(python3 tprint.py customer.id) \
	ABS_PUB_KEY=$(python3 tprint.py pubkey.path) \
	./gradlew $@
