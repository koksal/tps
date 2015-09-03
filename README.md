  [Cytoscape]: http://www.cytoscape.org/
  [Java Development Kit 8]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
  [sbt]: https://github.com/sbt/sbt
  [sbt-extras]: https://github.com/paulp/sbt-extras
  [ScalaZ3]: https://github.com/epfl-lara/ScalaZ3
  [sif]: http://wiki.cytoscape.org/Cytoscape_User_Manual/Network_Formats

# TPS: Temporal Pathway Synthesizer

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
   command-line arguments. To run TPS using the provided data, use the
   following command:

        ./scripts/run \
          --network data/networks/input-network.tsv \
          --timeseries data/timeseries/median-time-series.tsv \
          --firstscores data/timeseries/p-values-first.tsv \
          --prevscores data/timeseries/p-values-prev.tsv \
          --partialmodel data/resources/kinase-substrate-interactions.sif \
          --peptidemap data/timeseries/peptide-mapping.tsv \
          --source EGF_HUMAN \
          --threshold 0.01
   This command will generate a network file called `output.sif` in the current folder.

## Command-line arguments

```
$ ./scripts/run --help
tps 2.0
Usage: tps [options]

  --network <value>
        input network file
  --timeseries <value>
        input time series file
  --firstscores <value>
        significance scores for time series points w.r.t. first time point
  --prevscores <value>
        significance scores for time series points w.r.t. previous time point
  --partialmodel <value>
        input partial model given as a network
  --peptidemap <value>
        peptide protein mapping
  --outlabel <value>
        prefix that will be added to output file names
  --outfolder <value>
        folder that output files should be created in
  --source <value>
        network source
  --threshold <value>
        significance score threshold
  --solver <value>
        solver (naive, bilateral or dataflow)
  --slack <value>
        limit path lengths from source to each node (only for symbolic solvers)
  --bitvect <value>
        use bitvectors for integer encoding
  --no-connectivity
        do not assert conectivity constraints
  --no-temporality
        do not assert temporal constraints
  --no-monotonicity
        do not assert monotonicity constraints
  --help
        print this help message
```

## Output

TPS outputs a Simple Interaction Format ([sif]) file that summarizes the valid
pathway models.  The sif file can be imported into [Cytoscape] to visualize
the network.  Each line has the form:
```
ProteinA <relationship type> ProteinB
```
The TPS relationship types are:
* **A**: ProteinA activates ProteinB
* **I**: ProteinA inhibits ProteinB
* **N**: ProteinA regulates ProteinB but the edge sign is unknown
* **U**: an undirected edge between ProteinA and ProteinB

## Solvers

TPS uses by default a custom solver (`DataflowSolver`), but it also includes
two symbolic solvers (`NaiveSymbolicSolver` and `BilateralSolver`) that
implement the same functionality as the custom solver. 

We recommend using the default solver, which is the most recent and fastest of
all three. Meanwhile, if you would like to use either of the two symbolic
solvers on OS X, you will need to replace the `scalaz3.jar` with a packaged
version of [ScalaZ3] built on the computer you will run TPS on. Instructions
for building ScalaZ3 can be found on its project page.

## Authors

* Ali Sinan Koksal
* Anthony Gitter
* Kirsten Beck
* Aaron McKenna
* Saurabh Srivastava
* Nir Piterman
* Rastislav Bodik
* Alejandro Wolf-Yadlin
* Ernest Fraenkel
* Jasmin Fisher
