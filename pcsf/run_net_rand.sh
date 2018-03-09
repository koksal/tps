#!/bin/bash

NETWORK_FILE='data/networks/phosphosite-irefindex13.0-uniprot-with-header.txt'
OUTDIR='randomized_networks'

python pcsf/generate_randomized_networks.py \
  --network $NETWORK_FILE \
  --outdir $OUTDIR \
  --copies 100
