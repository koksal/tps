  [Omics Integrator]: https://github.com/fraenkel-lab/OmicsIntegrator

# PCSF-TPS pipeline
This directory contains scripts to run the Prize-Collecting Steiner Forest
(PCSF) and Temporal Pathway Synthesizer (TPS) algorithms back-to-back.  All
scripts must be run from the main `tps` directory not this `pcsf` subdirectory.
For example, call `pcsf/submit_wrapper.sh` not `submit_wrapper.sh`.

## Requirements
The PCSF-TPS pipeline initially supports only Linux and Mac OS X.
* [Omics Integrator]
* msgsteiner (see Omics Integrator for installation instructions)
* pandas Python package (optional)
* R package [`BiRewire`](https://doi.org/doi:10.18129/B9.bioc.BiRewire) (network randomization only)

The pandas package is only required to generate PCSF prizes from the TPS
input files with `generate_prizes.sh`.

## Running on example data
To run PCSF and generate an input network for TPS using the EGF
response example dataset:

1. Install Omics Integrator, msgsteiner, and optionally pandas.
2. Optionally run `generate_prizes.sh` to generate prizes for PCSF from the
 TPS score files and peptide-protein map file.  This will overwrite the
 existing prize file `data/pcsf/egfr-prizes.txt` and is only used to
 illustrate the script usage.
3. Edit the `oipath` and `msgsteinerpath` variables in `submit_wrapper.sh`
 with the paths where Omics Integrator and msgsteiner were installed.
 Optionally edit the `condor_submit submit_PCSF.sub` line if you are not using
 the HTCondor queueing system.
4. Run `submit_wrapper.sh` to generate a family of Steiner forests and a script
 that summarizes them by taking the union of all forests (default
 script name `summarize_forests.sh`).
5. Run `summarize_forests.sh` to generate the tab-separated union graph for TPS.
6. Run TPS (not yet part of the pipeline).

## Running on new data
To run PCSF with new data follow the same steps for running PCSF on the example
data with these additional steps:

1. If starting from TPS input files instead of a PCSF prize file, edit
 `generate_prizes.sh` to generate PCSF prizes from the TPS files.  Skip
 this step if the PCSF prizes are already available.
2. Edit the `prizetype`, `prizepath`, `edgefile`, or `sources` variables in
 `submit_wrapper.sh` to use a different protein prize file, protein-protein
 interaction network, or source node file.
3. Edit the `b`, `m`, and `w` variables in `submit_wrapper.sh` to set beta, mu,
 and omega to the optimal PCSF parameters.  The PCSF parameter sweep is not
 directly supported by this pipeline.
4. Optional edit the `outpath` variable in `submit_wrapper.sh` to store
 the results in a different directory or the `sumscript` variable to
 generate a different summarization script.

## Additional options
Other elements in the `submit_wrapper.sh` can be modified if needed but
can generally be left at their default values.

- You can change the number of forests in the family of Steiner forests
 (the default is 100, though 1000 or more can be preferable when running in
 parallel on a cluster).
- You can edit the msgsteiner parameters `D` and `g` (see [Omics Integrator]
 for details).
- You can edit the `r` parameter to increase the edge noise, which will
 lead to more diverse forests in the family of Steiner forest solutions.

## Running on permuted data
The full PCSF-TPS pipeline can be run on randomized protein prizes by
permuting the peptide-protein map and regenerating the prizes.  The script
`permuted_wrapper.sh` initiates the entire run, running PCSF and TPS
on multiple times on different shuffled peptide-protein maps.  To run the
pipeline:

1. Edit the variables in the script `permuted_wrapper.sh` to set the PCSF input
 data, TPS input data, and parameters such as the number of random runs to
 execute and the number of Steiner forests to generate in each run.
2. Run `permuted_wrapper.sh`, which will generate the random peptide-protein
 map files and submit `submit_permuted.sub` to the HTCondor queueing system.
 The rest of the pipeline will run automatically.
3. `submit_permuted.sub` launches the desired number of jobs, each of which
 will run `run_permuted_pipeline.sh` with a different shuffled peptide-protein
 map file.  Each job's HTCondor `Process` variable is used to track which
 version of the shuffled data to use as input.
4. `run_permuted_pipeline.sh` will generate prizes for PCSF, run PCSF multiple
 times to generate a family of forests, summarize the family of forests to
 produce a single input network for TPS, and run TPS.

## Running on bootstrapped data
The full PCSF-TPS pipeline can be run on bootstrapped peptide-level data by
subsampling the three types of peptide scores and regenerating protein prizes.
The script `bootstrap_wrapper.sh` initiates the entire run, running PCSF and TPS
on multiple times on different subsampled datasets.  To run the pipeline:

1. Edit the variables in the script `bootstrap_wrapper.sh` to set the PCSF input
 data, TPS input data, and parameters such as the number of random runs to
 execute, the fraction of peptide scores to retain, and the number of Steiner
 forests to generate in each run.
2. Run `bootstrap_wrapper.sh`, which will generate the subsampled peptide-level
 files and submit `submit_bootstrap.sub` to the HTCondor queueing system. The
 rest of the pipeline will run automatically.
3. `submit_bootstrap.sub` launches the desired number of jobs, each of which
 will run `run_bootstrap_pipeline.sh` with a different set of subsampled files.
 Each job's HTCondor `Process` variable is used to track which version of the
 subsampled files to use as input.
4. `run_bootstrap_pipeline.sh` will generate prizes for PCSF, run PCSF multiple
 times to generate a family of forests, summarize the family of forests to
 produce a single input network for TPS, and run TPS.

Note that the subsampling script `subsample_peptides.py` expects the
`firstfile`, `prevfile`, and `tsfile` inputs to all begin with a single header
row, as in the example input files in the `data/timeseries` subdirectory.

## Running on randomized networks
The full PCSF-TPS pipeline can be run on a randomized background network.
Unlike the other permutation and bootstrap pipelines, two scripts are required.
One generates the randomized networks, and the other runs the PCSF and TPS
pipeline on those networks.  To run the pipeline:

1. Edit the arguments in the script `run_net_rand.sh` to specify the original
 background network and the number of randomized copies to generate.  The script
 calls `generate_randomized_networks.py`, a wrapper around BiRewire, and
 randomizes the directed and undirected edges in the background network
 separately.  Each randomized network copy produces two output files: a
 background network with directed and undirected edges for PCSF and a partial
 model file with the directed edges for TPS.
2. Edit the variables in the script `net_rand_wrapper.sh` to set the PCSF
 prizes, TPS input data, randomized networks, and parameters such as the number
 of random runs to execute and the number of Steiner forests to generate in each
 run.  The number of random runs `netrandcopies` should match the number of
 randomized network copies from step 1.
3. Run `net_rand_wrapper.sh`, which will submit `submit_net_rand.sub` to the
 HTCondor queueing system. The rest of the pipeline will run automatically.
4. `submit_net_rand.sub` launches the desired number of jobs, each of which will
 run `run_net_rand_pipeline.sh` with a different randomized network.  Each job's
 HTCondor `Process` variable is used to track which randomized network to use as
 input.
5. `run_net_rand_pipeline.sh` will run PCSF multiple times to generate a family
 of forests, summarize the family of forests to produce a single input network
 for TPS, and run TPS.

Note that the randomized networks from BiRewire can contain self-edges.

## Usage messages
```
usage: generate_prizes.py [-h] --firstfile FIRSTFILE --prevfile PREVFILE
                          --mapfile MAPFILE --outfile OUTFILE

Compute peptide prizes from the TPS first and previous scores files and map
them to protein prizes. See the TPS readme for the expected file formats.

optional arguments:
  -h, --help            show this help message and exit
  --firstfile FIRSTFILE
                        The path and filename of the TPS firstscores file
  --prevfile PREVFILE   The path and filename of the TPS prevscores file
  --mapfile MAPFILE     The path and filename of the TPS peptidemap file
  --outfile OUTFILE     The path and filename of the output prize file.
```

```
usage: generate_randomized_networks.py [-h] --network NETWORK
                                       [--outdir OUTDIR] [--copies COPIES]

Randomize background network.

optional arguments:
  -h, --help         show this help message and exit
  --network NETWORK  Background network file in tsv format.
  --outdir OUTDIR    Output directory for randomized files
  --copies COPIES    The number of subsampled copies to generate (default 10).
```

```
usage: permute_proteins.py [-h] --mapfile MAPFILE [--outdir OUTDIR]
                           [--copies COPIES] [--seed SEED]

Shuffle the protein(s) that map to each peptide. Creates the specified number
of peptide-protein map files. See the TPS readme for the expected file format.

optional arguments:
  -h, --help         show this help message and exit
  --mapfile MAPFILE  The path and filename of the original TPS peptidemap
                     file, which must contain a file extension.
  --outdir OUTDIR    The path of the output directory for the permuted map
                     files (default is the directory of the mapfile).
  --copies COPIES    The number of shuffled copies to generate (default 10).
  --seed SEED        A seed for the pseudo-random number generator for
                     reproducibility.
```

```
usage: subsample_peptides.py [-h] --firstfile FIRSTFILE --prevfile PREVFILE
                             --tsfile TSFILE [--outdir OUTDIR]
                             [--fraction FRACTION] [--copies COPIES]
                             [--seed SEED]

Subsample the peptides in the time series and score files for bootstrapping.
See the TPS readme for the expected file formats.

optional arguments:
  -h, --help            show this help message and exit
  --firstfile FIRSTFILE
                        The path and filename of the TPS firstscores file
  --prevfile PREVFILE   The path and filename of the TPS prevscores file
  --tsfile TSFILE       The path and filename of the TPS timeseries file
  --outdir OUTDIR       The path of the output directory for the subsamples
                        files (default is the directory of the input files).
  --fraction FRACTION   The fraction of peptides to keep in the subsampled
                        datasets (default 0.9).
  --copies COPIES       The number of subsampled copies to generate (default
                        10).
  --seed SEED           A seed for the pseudo-random number generator for
                        reproducibility.
```

```
usage: summarize_sif.py [-h] --indir INDIR [--pattern PATTERN]
                        [--siflist SIFLIST] [--prizefile PRIZEFILE] --outfile
                        OUTFILE [--hubnode HUBNODE] [--cyto28]

Summarize a collection of Steiner forests

optional arguments:
  -h, --help            show this help message and exit
  --indir INDIR         The path to the directory that contains sif files.
  --pattern PATTERN     The filename pattern of the sif files in indir. Not
                        needed if a siflist is provided instead
  --siflist SIFLIST     A list of sif files in indir delimited by '|'. Not
                        used if a pattern is provided.
  --prizefile PRIZEFILE
                        The path and filename prefix of the prize file
                        (optional). Assumes the same prize file was used for
                        all forests.
  --outfile OUTFILE     The path and filename prefix of the output. Does not
                        include an extension.
  --hubnode HUBNODE     The name of a hub node in the network (optional). The
                        degree of this node will be reported.
  --cyto28              This flag will generate node and edge frequency
                        annotation files in the Cytoscape 2.8 format instead
                        of the default Cytoscape 3 style.
```
