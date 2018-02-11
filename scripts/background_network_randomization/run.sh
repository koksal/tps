#!/bin/bash

NETWORK_FILE='data/networks/directed-pin-with-resource-edges.tsv'

python scripts/background_network_randomization/generate_randomized_networks.py \
  --network $NETWORK_FILE \
  --outdir . \
  --copies 3
