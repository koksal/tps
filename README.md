  [Cytoscape]: http://www.cytoscape.org/
  [iRefIndex]: http://irefindex.org/
  [Java Development Kit 8]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
  [Omics Integrator]: http://fraenkel.mit.edu/omicsintegrator
  [PhosphoSitePlus]: http://www.phosphosite.org/
  [sbt]: https://github.com/sbt/sbt
  [sbt-extras]: https://github.com/paulp/sbt-extras
  [ScalaZ3]: https://github.com/epfl-lara/ScalaZ3
  [SIF]: http://wiki.cytoscape.org/Cytoscape_User_Manual/Network_Formats
  [Tukey's Honest Significant Difference test]: https://en.wikipedia.org/wiki/Tukey%27s_range_test
  [TukeyHSD]: https://stat.ethz.ch/R-manual/R-patched/library/stats/html/TukeyHSD.html

# TPS: Temporal Pathway Synthesizer [![Circle CI](https://circleci.com/gh/koksal/tps.svg?style=svg)](https://circleci.com/gh/koksal/tps)

TPS is a tool for combining time series global phosphoproteomic data and
protein-protein interaction networks to reconstruct the vast signaling pathways
that control post-translational modifications.

## Requirements

TPS runs on both Linux and OS X. The only requirement is:
* [Java Development Kit 8].

## Installation and sample usage

TPS is built and run using the command-line interface. To use TPS, follow these
steps:

1. Download the code:

        git clone https://github.com/koksal/tps.git
2. Browse to the root project folder:

        cd tps
3. Invoke `./scripts/run`. The first time this script is run, it will download
   [sbt-extras], which is a script for running the build tool [sbt]. After sbt
   is downloaded, the script will build the code and run TPS with the given
   command-line arguments. To run TPS using the provided data, copy and paste
   the following command into the terminal:

        ./scripts/run \
          --network data/networks/input-network.tsv \
          --timeseries data/timeseries/median-time-series.tsv \
          --firstscores data/timeseries/p-values-first.tsv \
          --prevscores data/timeseries/p-values-prev.tsv \
          --partialmodel data/resources/kinase-substrate-interactions.sif \
          --peptidemap data/timeseries/peptide-mapping.tsv \
          --source EGF_HUMAN \
          --threshold 0.01
   This command will generate, in the current folder:
   - a network file named `output.sif`
   - a tab-separated file named `activity-windows.tsv` 

   The output files are described in the **Output** section.

## Command-line arguments

### Required arguments

- `--network <file>`: Input network file in TSV format, where each row defines an undirected edge.
- `--timeseries <file>`: Input time series file in TSV format. The first line defines the time point labels, and each subsequent line corresponds to one time series profile.
- `--firstscores <file>`: Input file that contains significance scores for each time point of a profile (except the first time point), with respect to the first time point of the profile.
- `--prevscores <file>`: Similar to `--firstscores`, an input file that gives significance scores for each time point (except the first one), with respect to the previous time point.
- `--source <value>`: Identifier for the network source node. Multiple source nodes can be provided by repeating the argument multiple times. For example, `--source <node1> --source <node2> --source <node3>`.
- `--threshold <value>`: Threshold value for significance scores, above which measurements are considered non-significant.

### Optional arguments

- `--partialModel <file>`: Input partial model file given as a signed directed SIF network. Each line corresponds to a directed interaction, where the relationship type can be **N** (directed, unsigned edge), **A** (directed activation edge), or **I** (directed inhibition edge). Multiple partial model files can be provided.
- `--peptidemap <file>`: Input file in TSV format that defines a mapping between time series profile identifiers and input network node identifiers. A profile can be mapped to more than one node, in which case the second column is a pipe-separated list of node identifiers.
- `--outlabel <value>`: Prefix string to be added to all output files.
- `--outfolder <value>`: Folder in which the output files should be generated. By default, output files are generated in the current directory.
- `--solver <value>`: Solver to use (`naive`, `bilateral`, or `dataflow`). The default, recommended solver is `dataflow`. Both `naive` and `bilateral` are symbolic solvers and use the Z3 backend. (See the **Solvers** section for notes related to the symbolic solvers.) 
- `--slack <value>`: Integer value for limiting the length of paths from the source to any node to *n* + *k*, where *n* is the length of the shortest path from the source to the node in the undirected network, and *k* is the given slack value. This only applies to the symbolic solvers.
- `--bitvect <value>`: Use bitvector encoding for representing integers when using the symbolic solvers, with bitvectors of the given integer length.
- `--no-connectivity`: Do not use connectivity constraints.
- `--no-temporality`: Do not use temporal constraints.
- `--no-monotonicity`: Do not use monotonicity constraints when inferring activity intervals for time series data.

## Preparing input files

We recommend the following strategies for preparing the required input files:
- `--network <file>`: The network should be a subnetwork of a protein-protein
interaction network that connects the phosphorylated proteins to the source node(s).
The [Omics Integrator] implementation of the Prize-Collecting Steiner Forest
algorithm can produce such a subnetwork. To generate more general subnetworks
instead of tree-structured graphs, run Omics Integrator with the option to add random
noise to edge weights and merge the graphs output by each randomized run.  Omics Integrator
writes the network in a three column tab-separated format.  The second column, the
interaction type, must be removed before providing the file to TPS.
- `--timeseries <file>`: TPS expects a single intensity for each peptide at each time point,
which can be calculated by taking the median intensity over all mass spectrometry replicates.
TPS allows missing data, which should be denoted by a non-numeric value such as **N/A**
or an empty string. This file must contain a header row, which specifies the time point
labels.
- `--firstscores <file>`: Significance scores can be naively computed with t-tests
comparing the phosphorylation intensity at each time point and the first time point.
An alternative option is to account for the comparisons of multiple pairs of time
points using [Tukey's Honest Significant Difference test], which is implemented as [TukeyHSD]
in R. This test compares all pairs of time points, from which the comparisons to the
first time point can be extracted. This file should not contain a header row, and if a
header row is provided it should be commented out with a leading **#** character. If
there are *t* time points in the `--timeseries <file>`, this file should contain
*t* - 1 significance score columns.
- `--prevscores <file>`: Significance scores can be computed in the same manner as the
`--firstscores <file>` except the scores should be based on comparisons of the current
time point and the preceding time point. The file format is the same as the
`--firstscores <file>`.

## Output

### Summary network

TPS outputs a Simple Interaction Format ([SIF]) file `output.sif` that
summarizes the valid pathway models.  The SIF file can be imported into
[Cytoscape] to visualize the network.  Each line has the form:
```
ProteinA <relationship type> ProteinB
```
The TPS relationship types are:
* **A**: ProteinA activates ProteinB
* **I**: ProteinA inhibits ProteinB
* **N**: ProteinA regulates ProteinB but the edge sign is unknown
* **U**: an undirected edge between ProteinA and ProteinB

### Activity windows

TPS also produces a tab-separated file `activity-windows.tsv` that lists, for
each node in the expanded input network, one of four possible activity types
per time point:
* **activation**: the peptide may be activated at the given time point
* **inhibition**: the peptide may be inhibited at the given time point
* **ambiguous**: the peptide may be either activated or inhibited at the given time point
* **inactive**: the peptide is inactive at the given time point

## Solvers

TPS uses by default a custom solver (`DataflowSolver`), but it also includes
two symbolic solvers (`NaiveSymbolicSolver` and `BilateralSolver`) that
implement the same functionality as the custom solver. 

We recommend using the default solver, which is the most recent and fastest of
all three. Meanwhile, if you would like to use either of the two symbolic
solvers on OS X, you will need to replace the `scalaz3.jar` with a packaged
version of [ScalaZ3] built on the computer you will run TPS on. Instructions
for building ScalaZ3 can be found on its project page.

## Example data

The example dataset included with TPS is a phosphoproteomic time course of the
cellular response to EGF stimulation. This dataset will be described in a
forthcoming manuscript. Please refrain from publishing analyses of this
dataset until the manuscript appears.

The example network was produced by [Omics Integrator] run on a network
of [iRefIndex] and [PhosphoSitePlus] interactions.


## Authors

* Ali Sinan Koksal
* Anthony Gitter
* Kirsten Beck
* Aaron McKenna
* Saurabh Srivastava
* Rastislav Bodik
* Alejandro Wolf-Yadlin
* Ernest Fraenkel
* Jasmin Fisher
