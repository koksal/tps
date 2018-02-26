#!/bin/bash
# Submit PCSF-TPS runs, each using a different randomized network.
# The randomized networks must be generated before running this script.
# Execute PCSF runs with different seeds for random edge noise.
# Summarize the PCSF runs and run TPS on on the union network.
# Sets up a new configuration file if needed and uses
# environment variables to pass other arguments to forest.py

# Set the output directory for PCSF, TPS, and HTCondor output
export outpath=results
mkdir -p $outpath

# Set the TPS input files, which will be passed to TPS and used to
# generate prizes for PCSF
export tpsfirstscores=data/timeseries/p-values-first.tsv
export tpsprevscores=data/timeseries/p-values-prev.tsv
export tpspeptidemap=data/timeseries/peptide-mapping.tsv
# The other TPS inputs not needed to generate prizes for PCSF
# The source nodes are set below
export tpstimeseries=data/timeseries/median-time-series.tsv
export tpsthreshold=0.01

# The prefixes for the previously randomized background network
# and TPS partial model file, which is re-derived for each random
# background.  Here the randomized networks are stored in the
# output directory.
export tpspartialmodelprefix=${outpath}/phosphosite-irefindex13.0-uniprot-with-header-partial-model-randomized
export networkprefix=${outpath}/phosphosite-irefindex13.0-uniprot-with-header-randomized

# Set the code paths for the Omics Integrator and msgsteiner dependencies
## This is the directory that contains scripts/forest.py
export oipath=.
## This is the path to the msgsteiner executable, including the executable name
export msgsteinerpath=.

# Parameters set during the parameter sweep
b=0.55
m=0.008
w=0.1

# The prize file does not need to be regenerated when running with randomized networks
export prizefile=data/pcsf/egfr-prizes.txt
export prizename=egfr-prizes
# A file listing the protein names that should be treated as source nodes,
# including the path.  These will be used for PCSF and TPS.
## This example uses EGF as the source node for EGF stimulation response
export sources=data/pcsf/egfr-sources.txt

# The following three parameters can typically be left at these default values
# Depth from root of tree
D=10
# Convergence parameter
g=1e-3
# Noise to compute a family of solutions
r=0.01

# Create the configuration file, removing an older copy of the file if it exists
# Only one copy is needed for all of the PCSF runs on randomized networks
mkdir -p ${outpath}/conf
filename=${outpath}/conf/conf_w${w}_b${b}_D${D}_m${m}_r${r}_g${g}.txt
rm -f $filename
touch $filename
printf "w = ${w}\n" >> $filename
printf "b = ${b}\n" >> $filename
printf "D = ${D}\n" >> $filename
printf "mu = ${m}\n" >> $filename
printf "r = ${r}\n" >> $filename
printf "g = ${g}\n" >> $filename

# Set the remaining PCSF environment variables
export conf=$filename
export beta=$b
export mu=$m
export omega=$w
# The number of forests to generate and merge into the final TPS seed network
export forests=100

# Set the number of network randomization runs
export netrandcopies=100

# Submit netrandcopies of the job to HTCondor
# Could replace this with a submission to a different queueing system
# (e.g. qsub instead of condor_submit) but running the pipelines locally
# is not recommended due to the long runtime of executing them serially
condor_submit pcsf/submit_net_rand.sub
