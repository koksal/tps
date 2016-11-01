#!/bin/bash
# Submit PCSF runs with different seeds for random edge noise
# Sets up a new configuration file if needed and uses
# environment variables to pass other arguments to forest.py

# Set the output directory for PCSF and HTCondor output
export outpath=./results
mkdir -p $outpath

# Set the code paths for the Omics Integrator and msgsteiner dependencies
## This is the directory that contains scripts/forest.py
export oipath=.
## This is the path to the msgsteiner executable, including the executable name
export msgsteinerpath=.

# Parameters set during the parameter sweep
b=0.55
m=0.008
w=0.1

# Prize filename prefix (assume a .txt extension follows, e.g. firstprev.txt)
# It will be used to create an output prefix for the Steiner forest networks
## This example uses the prizes derived from the statistical significance of each
## protein's phosphorlyation intensity after EGF stimulation compared to its
## phosphorylation at the previous and first time points
export prizetype=egfr-prizes
# The path to the prize file above
export prizepath=../data/pcsf
# The PPI network, including the path
## This example uses a combination of PhosphoSitePlus and iRefIndex interactions
## with UniProt entry name identifiers
export edgefile=../data/networks/phosphosite-irefindex13.0-uniprot.txt
# A file listing the protein names that should be treated as source nodes,
# including the path
## This example uses EGF as the source node for EGF stimulation response
export sources=../data/pcsf/egfr-sources.txt

# The following three parameters can typically be left at these default values
# Depth from root of tree
D=10
# Convergence parameter
g=1e-3
# Noise to compute a family of solutions
r=0.01

# Create the configuration file, removing an older copy of the file if it exists
mkdir -p conf
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

# Generate a wrapper script to summarize the family of forests
# This must be run after all PCSF runs terminate
# HTCondor can manage these dependencies with the Directed Acyclic Graph Manager
# but this strategy generalizes to other setups
# Set the name of the summarization script, which overwrites an existing
# file with the same name
# The script assumes that the summarization Python code resides in the same
# directory
sumscript=summarize_forests.sh
# A filename pattern used to collect all of the forest output files
pattern=${prizetype}_beta${beta}_mu${mu}_omega${omega}
rm -f $sumscript
touch $sumscript
printf "#!/bin/bash\n" >> $sumscript
printf "#Summarize a family of Steiner forests\n" >> $sumscript
printf "python summarize_sif.py --indir ${outpath} --pattern ${pattern}*optimalForest.sif --prizefile ${prizepath}/${prizetype}.txt --outfile ${outpath}/${pattern}_summary\n" >> $sumscript
chmod u+x $sumscript
