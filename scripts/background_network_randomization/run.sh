#!/bin/bash

NETWORK_FILE="data/networks/directed-pin-with-resource-edges.tsv"

python scripts/randomize_background_network.py \
  --network $NETWORK_FILE \
  --copies 2
