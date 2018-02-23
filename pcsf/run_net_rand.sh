#!/bin/bash

NETWORK_FILE='data/networks/directed-pin-with-resource-edges.tsv'

python pcsf/generate_randomized_networks.py \
  --network $NETWORK_FILE \
  --outdir . \
  --copies 10
