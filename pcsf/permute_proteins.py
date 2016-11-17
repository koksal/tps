import os, sys
import random as rn
from argparse import ArgumentParser

__author__ = "Anthony Gitter"

def Main(arg_list):
    """Parse the arguments, which either come from the command line or a list
    provided by the Python code calling this function
    """
    parser = CreateParser()
    options = parser.parse_args(arg_list)

    assert options.mapfile is not None, "Must specify the mapfile"

    # Set the pseudo-random number generator seed if one was provided
    if options.seed is not None:
        rn.seed(options.seed)

    # Load the original peptide and protein lists
    peptides, proteins = LoadPeptideMap(options.mapfile)
    assert len(peptides) == len(proteins), "Error parsing the preptide-protein map"

    # Prepare the output file names
    filename, extension = os.path.splitext(options.mapfile)
    if options.outdir is not None:
        path, file_prefix = os.path.split(filename)
        filename = os.path.join(os.path.normpath(options.outdir), file_prefix)
    print "Writing shuffled map files of the form {}-shuffled<i>{}".format(filename, extension)

    # Shuffle and write the random protein order
    for index in range(1, options.copies + 1):
        shuffled = proteins
        rn.shuffle(shuffled)

        out_file = "{}-shuffled{}{}".format(filename, index, extension)
        with open(out_file, "w") as out_f:
            # Write the header
            out_f.write("peptide\tprotein(s)\n")
            for row in range(len(peptides)):
                out_f.write("{}\t{}\n".format(peptides[row], shuffled[row]))

    print "Wrote {} shuffled map files".format(options.copies)


def LoadPeptideMap(mapfile):
    """Parse the peptide-to-protein map file.  The peptide is in the first column
    and the protein or proteins are in the second.  The file contains a header row.
    A peptide may map to a set of pipe-delimited proteins.  A peptide may also
    appear multiple times, and the set of proteins in each appearance
    will be treated independently.

    Return: a list of preptides and a list of protein groups in the order
    they appeared in the file
    """
    peptides = []
    proteins = []

    # Load two lists
    with open(mapfile) as map_f:
        # Skip the header
        map_f.readline()
        for line in map_f:
            parts = line.strip().split("\t")
            assert len(parts) == 2, "Expected tab-delimited peptide and protein (or pipe-delimited proteins) on each line"

            peptides.append(parts[0])
            proteins.append(parts[1])

    print "Loaded {} peptide-protein pairs".format(len(peptides))
    return peptides, proteins


def CreateParser():
    """Setup the option parser"""
    parser = ArgumentParser(description="Shuffle the protein(s) that map to each peptide.  Creates the specified number of peptide-protein map files.  See the TPS readme for the expected file format.")
    parser.add_argument("--mapfile", type=str, dest="mapfile", help="The path and filename of the original TPS peptidemap file, which must contain a file extension.", default=None, required=True)
    parser.add_argument("--outdir", type=str, dest="outdir", help="The path of the output directory for the permuted map files (default is the directory of the mapfile).", default=None, required=False)
    parser.add_argument("--copies", type=int, dest="copies", help="The number of shuffled copies to genereate (default 10).", default=10, required=False)
    parser.add_argument("--seed", type=int, dest="seed", help="A seed for the pseudo-random number generator for reproducibility.", default=None, required=False)
    return parser


if __name__ == "__main__":
    """Use the command line arguments to setup the options
    (the same as the default ArgumentParser behavior)
    """
    Main(sys.argv[1:])
