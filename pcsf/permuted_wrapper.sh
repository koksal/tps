#!/bin/bash
# Permute the peptide-protein mapping to generate random PCSF prizes.
# Submit PCSF-TPS runs, each using a different permuated mapping.
# Execute PCSF runs with different seeds for random edge noise.
# Summarize the PCSF runs and run TPS on the union network.
# Sets up a new configuration file if needed and uses
# environment variables to pass other arguments to forest.py

# Set the output directory for PCSF, TPS, and HTCondor output
export outpath=results
mkdir -p $outpath

# Set the TPS input files, which will be passed to TPS and used to
# generate prizes for PCSF
export tpsfirstscores=data/timeseries/p-values-first.tsv
export tpsprevscores=data/timeseries/p-values-prev.tsv
# The other TPS inputs not needed to generate prizes for PCSF
# The source nodes are set below
export tpstimeseries=data/timeseries/median-time-series.tsv
export tpspartialmodel=data/resources/kinase-substrate-interactions.sif
export tpsthreshold=0.01

# The peptide map path and filename is split into components to make it easier
# to automatically build the path and filenames of the shuffled peptide maps
export peptidemappath=data/timeseries
export peptidemapprefix=peptide-mapping
export peptidemapext=.tsv
peptidemap=${peptidemappath}/${peptidemapprefix}${peptidemapext}

# Set the code paths for the Omics Integrator and msgsteiner dependencies
## This is the directory that contains scripts/forest.py
export oipath=.
## This is the path to the msgsteiner executable, including the executable name
export msgsteinerpath=.

# Parameters set during the parameter sweep
b=0.55
m=0.008
w=0.1

# Prize filename prefix
# It will be used to to create a prize file for PCSF from the TPS input score
# files and the permuted peptide-protein map and will also create an output prefix
# for the Steiner forest networks
# The prize files will be written in the output directory
export prizename=egfr-prizes
# The PPI network, including the path
## This example uses a combination of PhosphoSitePlus and iRefIndex interactions
## with UniProt entry name identifiers
export edgefile=data/networks/phosphosite-irefindex13.0-uniprot.txt
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
# Only one copy is needed for all of the PCSF runs on permuted prizes
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
export forests=10

# Set the seed for permuting prizes and the number of permuted copies
permuteseed=2016
export permutecopies=10

# Permute the peptide-protein mapping
python pcsf/permute_proteins.py --mapfile=$peptidemap \
	--outdir=$outpath \
	--copies=$permutecopies \
	--seed=$permuteseed

# Submit permutecopies of the job to HTCondor
# Could replace this with a submission to a different queueing system
# (e.g. qsub instead of condor_submit) but running the pipelines locally
# is not recommended due to the long runtime of executing them serially
condor_submit pcsf/submit_permuted.sub
