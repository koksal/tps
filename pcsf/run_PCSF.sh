#!/bin/bash
# Run PCSF using the parameters set in the wrapper file

echo _CONDOR_JOB_IWD $_CONDOR_JOB_IWD
echo Cluster $cluster
echo Process $process
echo RunningOn $runningon

prizefile=${prizepath}/${prizetype}.txt
outlabel=${prizetype}_beta${beta}_mu${mu}_omega${omega}_seed${seed}

# Create the Forest command
# Do not run with noisy edges because the r parameter in the configuration
# file is already being used to add edge noisy with msgsteiner
CMD="python $oipath/scripts/forest.py \
	-p $prizefile \
	-e $edgefile \
	-c $conf \
	-d $sources \
	--msgpath=$msgsteinerpath \
	--outpath=$outpath \
	--outlabel=$outlabel \
	--cyto30 \
	--noisyEdges=0 \
	-s $seed"

echo $CMD
$CMD

# Track which version was run
echo "Omics Integrator version:"
cd $oipath
python setup.py --version
