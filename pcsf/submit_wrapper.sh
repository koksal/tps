#!/bin/bash
# Submit PCSF runs with different seeds for random edge noise
# Sets up a new configuration file if needed and uses
# environment variables to pass other arguments to forest.py

# Set the output directory for PCSF and HTCondor output
export outpath=./results
mkdir -p $outpath

# Set the code paths for the Omics Integrator and msgsteiner dependencies
# This directory contains scripts/forest.py
export oipath=.
# This is the path to the msgsteiner executable, including the executable name
export msgsteinerpath=.

# Parameters set during the parameter sweep
b=0.55
m=0.008
w=0.1

# Prize filename prefix (assume a .txt extension follows, e.g. firstprev.txt)
# This example refers to a prize file derived from comparisons of phosphorylation
# at some time t with the previous and first time points
prizes=firstprev
# The path to the prize file above
export prizepath=.
# The PPI network, including the path
export edgefile=edges.txt
# A file listing the protein names that should be treated as source nodes,
# including the path
export sources=sources.txt

# The following three parameters can typically be left at these default values
# Depth from root of tree
D=10
# Convergence parameter
g=1e-3
# Noise to compute a family of solutions
r=0.01

# Create the configuration file, removing an older copy of the file if it exists
filename=conf/conf_w${w}_b${b}_D${D}_m${m}_r${r}_g${g}.txt
rm -f $filename
touch $filename
printf "w = ${w}\n" >> $filename
printf "b = ${b}\n" >> $filename
printf "D = ${D}\n" >> $filename
printf "mu = ${m}\n" >> $filename
printf "r = ${r}\n" >> $filename
printf "g = ${g}\n" >> $filename

# Set the remaining environment variables
export conf=$filename
export beta=$b
export mu=$m
export omega=$w
export prizetype=$prizes

# Use different seeds for each run, which will control the random edge noise
# Create a family of 100 forests
for s in $(seq 1 100)
do
	# Set the seed
	export seed=$s

	# Submit the job to HTCondor with the configuration file, params, and seed
	# Could replace this with a submission to a different queueing system
	# (e.g. qsub instead of condor_submit) or directly call run_PCSF.sh to
	# run locally.
	condor_submit submit_PCSF.sub
done
