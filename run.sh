#!/usr/bin/env bash
cd $(dirname $0)

env ABS_CUSTOMER_ID=$(python3 tprint.py customer.id) \
	ABS_PUB_KEY=$(python3 tprint.py pubkey.path) \
	./gradlew $@
