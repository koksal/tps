  [Omics Integrator]: https://github.com/fraenkel-lab/OmicsIntegrator

# PCSF-TPS pipeline
This directory is a work-in-progress that will contain scripts to run the
Prize-Collecting Steiner Forest (PCSF) and Temporal Pathway Synthesizer (TPS)
algorithms back-to-back.

## Requirements
The PCSF-TPS pipeline initially supports only Linux and Mac OS X.
* [Omics Integrator]
* msgsteiner (see Omics Integrator for installation instructions)
* pandas Python package (optional)

The pandas package is only required to generate PCSF prizes from the TPS
input files with `generate_prizes.sh`.

## Running on example data
To run PCSF and generate an input network for TPS using the EGF
response example dataset:

1. Install Omics Integrator, msgsteiner, and optionally pandas.
2. Optionally run `generate_prizes.sh` to generate prizes for PCSF from the
 TPS score files and peptide-protein map file.  This will overwrite the
 existing prize file `../data/pcsf/egfr-prizes.txt` and is only used to
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

## Usage messages
```
usage: permute_proteins.py [-h] --mapfile MAPFILE [--copies COPIES]
                           [--seed SEED]

Shuffle the protein(s) that map to each peptide. Creates the specified number
of peptide-protein map files in the same directory as the input file. See the
TPS readme for the expected file format.

optional arguments:
  -h, --help         show this help message and exit
  --mapfile MAPFILE  The path and filename of the original TPS peptidemap
                     file, which must contain a file extension.
  --copies COPIES    The number of shuffled copies to genereate (default 10).
  --seed SEED        A seed for the pseudo-random number generator for
                     reproducibility.
```

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
