#!/bin/bash
# A wrapper script to generate protein prizes from the TPS peptide-protein
# map, first scores file, and previous scores file.

# Generate prizes with the example data, overwriting the existing prize file
python generate_prizes.py --firstfile=../data/timeseries/p-values-first.tsv \
	--prevfile=../data/timeseries/p-values-prev.tsv \
	--mapfile=../data/timeseries/peptide-mapping.tsv \
	--outfile=../data/pcsf/egfr-prizes.txt
