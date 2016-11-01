  [Omics Integrator]: https://github.com/fraenkel-lab/OmicsIntegrator

# PCSF-TPS pipeline
This directory is a work-in-progress that will contain scripts to run the
Prize-Collecting Steiner Forest (PCSF) and Temporal Pathway Synthesizer (TPS)
algorithms back-to-back.

## Requirements
The PCSF-TPS pipeline initially supports only Linux.
* [Omics Integrator]
* msgsteiner (see Omics Integrator for installation instructions)

## Running on example data
To run PCSF and generate an input network for TPS using the EGF
response example dataset:
1. Install Omics Integrator and msgsteiner.
2. Edit the `oipath` and `msgsteinerpath` variables in `submit_wrapper.sh`
 with the paths where Omics Integrator and msgsteiner were installed.
 Optionally edit the `condor_submit submit_PCSF.sub` line if you are not using
 the HTCondor queueing system.
3. Run `submit_wrapper.sh` to generate a family of Steiner forests and a script
 that summarizes them by generating the union graph (default script name
 `summarize_forests.sh`).
4. Run `summarize_forests.sh` to generate the tab-separated union graph for TPS.
5. Run TPS (not yet part of the pipeline).

## Running on new data
To run PCSF with new data follow the same steps for running PCSF on the example
data with the following additional steps:
1. Edit the `prizetype`, `prizepath`, `edgefile`, or `sources` variables in
 `submit_wrapper.sh` to use a different protein prize file, protein-protein
 interaction network, or source node file.
2. Edit the `b`, `m`, and `w` variables in `submit_wrapper.sh` to set beta, mu,
 and omega to the optimal PCSF parameters.  The parameter sweep is not
 directly supported by this pipeline.
3. Optional edit the `outpath` variable in `submit_wrapper.sh` to store
 the results in a different directory or the `sumscript` variable to
 generate a different summarization script.

## Additional options
Other elements in the `submit_wrapper.sh` can be modified if needed, but
can generally be left at their default values.
- You can change the number of forests in the family of Steiner forests
 (default 100, though 1000 or more can be preferable when running in parallel
 on a cluster).
- You can edit the msgsteiner parameters `D` and `g` (see [Omics Integrator]
 for details).
- You can edit the `r` parameter to increase the edge noise, which will
 lead to more diverse forests in the family of Steiner forest solutions.
