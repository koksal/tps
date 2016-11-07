import sys
import pandas as pd
import numpy as np
from collections import defaultdict
from optparse import OptionParser

__author__ = "Anthony Gitter"

def Main(arg_list):
    """Parse the arguments, which either come from the command line or a list
    provided by the Python code calling this function
    """
    parser = CreateParser()
    (options, args) = parser.parse_args(arg_list)

    assert options.firstfile is not None, "Must specify the firstfile"
    assert options.prevfile is not None, "Must specify the prevfile"
    assert options.mapfile is not None, "Must specify the mapfile"
    assert options.outfile is not None, "Must specify the outfile"

    # Load the mapping from peptide ids to sets of protein ids
    pep_prot_map = LoadPeptideMap(options.mapfile)

    # Load the peptide scores and prizes
    merged_df = LoadScores(options.firstfile, options.prevfile)

    # Compute the protein prizes.  Take the maximum prize across all peptides
    # that map to the protein.  The same peptide can contribute to the prize of
    # multiple proteins if it maps to multiple protiens.

    # Default prize is 0.0
    prot_prizes = defaultdict(float)

    for index, row in merged_df.iterrows():
        # index is the peptide
        assert index in pep_prot_map, "No protein mapping for {}".format(index)
        prots = pep_prot_map[index]
        # The peptide can map to a set of proteins
        for prot in prots:
            # Keep the larger of the previous best prize for this protein and
            # the current prize
            prot_prizes[prot] = max(prot_prizes[prot], row["prize"])

    # Write the prizes to a file.
    print "Writing prizes for {} unique proteins to {}".format(len(prot_prizes), options.outfile)

    with open(options.outfile, "w") as out_f:
        for prot in sorted(prot_prizes.keys()):
            out_f.write("{}\t{}\n".format(prot, prot_prizes[prot]))


def LoadPeptideMap(mapfile):
    """Parse the peptide-to-protein map file.  The peptide is in the first column
    and the protein or proteins are in the second.  The file contains no header row.
    A peptide may map to a set of pipe-delimited proteins.  A peptide may also
    appear multiple times, and the map will include all proteins mapped to that
    peptide in any line.

    Return: dictionary mapping peptides to sets of proteins
    """
    pep_prot_map = defaultdict(set)
    unique_prots = set()

    with open(mapfile) as map_f:
        for line in map_f:
            parts = line.strip().split("\t")
            assert len(parts) == 2, "Expected tab-delimited peptide and protein (or pipe-delimited proteins) on each line"

            pep = parts[0]
            prots = parts[1].split("|")

            # Add all proteins that map to the peptide in this line to any
            # existing proteins that mapped to the peptide in previous lines
            pep_prot_map[pep].update(prots)
            # Add the proteins to the set of all unique observed proteins
            unique_prots.update(prots)

    print "Loaded {} unique peptides that map to {} unique proteins".format(len(pep_prot_map), len(unique_prots))
    return pep_prot_map

def LoadScores(firstfile, prevfile):
    """Load the first and previous scores.  For each peptide, compute a prize
    that is -log10(min p-value across all time points).  Assumes the scores
    are p-values or equivalaent scores in (0, 1].  Do not allow null or missing
    scores.

    Return: data frame with scores and prize for each peptide
    """
    first_df = pd.read_csv(firstfile, sep="\t", comment="#", header=None, index_col=0)
    prev_df = pd.read_csv(prevfile, sep="\t", comment="#", header=None, index_col=0)
    first_shape = first_df.shape
    assert first_shape == prev_df.shape, "First and previous score files must have the same number of peptides and time points"

    assert not first_df.isnull().values.any(), "First scores file contains N/A values.  Replace with 1.0"
    assert not prev_df.isnull().values.any(), "Previous scores file contains N/A values.  Replace with 1.0"

    print "Loaded {} peptides and {} scores in the first and previous score files".format(first_shape[0], first_shape[1])

    # Merge the two types of scores
    merged_df = pd.concat([first_df, prev_df], axis=1, join="outer")
    merged_shape = merged_df.shape
    assert merged_shape[0] == first_shape[0], "First and previous significance scores contain different peptides"
    assert merged_shape[1] == 2*first_shape[1], "Unexpected number of significance scores after merging first and previous scores"

    # Compute prizes
    merged_df["prize"] = merged_df.apply(CalcPrize, axis=1)
    return merged_df


def CalcPrize(row):
    """Compute the peptide prize as -log10(min p-value)"""
    return -np.log10(min(row))


def CreateParser():
    """Setup the option parser"""
    parser = OptionParser()
    parser.add_option("--firstfile", type="string", dest="firstfile", help="The path and filename of the TPS firstscores file", default=None)
    parser.add_option("--prevfile", type="string", dest="prevfile", help="The path and filename of the TPS prevscores file", default=None)
    parser.add_option("--mapfile", type="string", dest="mapfile", help="The path and filename of the TPS peptidemap file", default=None)
    parser.add_option("--outfile", type="string", dest="outfile", help="The path and filename of the output prize file.", default=None)
    return parser


if __name__ == "__main__":
    """Use the command line arguments to setup the options
    (the same as the default OptionParser behavior)
    """
    Main(sys.argv[1:])