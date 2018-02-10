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

    assert options.firstfile is not None, "Must specify the firstfile"
    assert options.prevfile is not None, "Must specify the prevfile"
    assert options.tsfile is not None, "Must specify the tsfile"

    assert options.fraction >= 0 and options.fraction <= 1.0, "The fraction must be in [0,1]"

    # Set the pseudo-random number generator seed if one was provided
    if options.seed is not None:
        rn.seed(options.seed)

    # Load the original peptide time series and score files
    headers, data = LoadPeptideData(options.firstfile, options.prevfile, options.tsfile)

    # Compute the length of the subsampled files
    subsampled_len = int(round(len(data)*options.fraction))
    assert subsampled_len > 0, "Must increase the fraction to subsample at least one row"
    assert subsampled_len == len(data), "Must decrease the fraction to subsample less than the total number of rows"
    print "Subsampling {} of {} rows".format(subsampled_len, len(data))

    # Prepare the output file names
    filename_list = []
    ext_list = []
    peptide_files = [options.firstfile, options.prevfile, options.tsfile]
    for peptide_file in peptide_files:
        filename, extension = os.path.splitext(peptide_file)
        if options.outdir is not None:
            path, file_prefix = os.path.split(filename)
            filename = os.path.join(os.path.normpath(options.outdir), file_prefix)
        filename_list.append(filename)
        ext_list.append(extension)
        print "Writing subsampled files of the form {}-{}-subsampled<i>{}".format(filename, options.fraction, extension)

    # Shuffle, subsample, and write the subsampled peptide data
    for copy_ind in range(1, options.copies + 1):
        shuffled = data
        rn.shuffle(shuffled)
        shuffled = shuffled[:subsampled_len]

        for file_ind in range(len(headers)):
            filename = filename_list[file_ind]
            extension = ext_list[file_ind]
            out_file = "{}-{}-subsampled{}{}".format(filename, options.fraction, copy_ind, extension)
            with open(out_file, "w") as out_f:
                # Write the header
                out_f.write(headers[file_ind])
                for data_lines in shuffled:
                    out_f.write(data_lines[file_ind])

    print "Wrote {} subsampled copies".format(options.copies)


def LoadPeptideData(firstfile, prevfile, tsfile):
    """Parse three tab-delimited files with peptide scores. Each begins with a
    header. Requires that the peptide ids are in the first column, are in
    the same order, and are identical in all three files.
    
    Return: a tuple of the header lines and a list of tuples for the remaining
    rows.
    """
    first_header, first_data, first_peptides = ParsePeptideFile(firstfile)
    prev_header, prev_data, prev_peptides = ParsePeptideFile(prevfile)
    ts_header, ts_data, ts_peptides = ParsePeptideFile(tsfile)
    
    # Confirm the same peptides are present in the same order
    assert first_peptides == prev_peptides, "Expected the same peptide ids in the same order"
    assert first_peptides == ts_peptides, "Expected the same peptide ids in the same order"

    return (first_header, prev_header, ts_header), zip(first_data, prev_data, ts_data)


def ParsePeptideFile(peptidefile):
    """Parse a tab-delimited file with peptide scores. Requires that peptide
    ids are in the first column.
    
    Returns: a tuple of the header row, the remaining rows, and the peptide
    ids.
    """
    with open(peptidefile) as pep_f:
        pep_lines = pep_f.readlines()
    pep_header = pep_lines[0]
    pep_data = pep_lines[1:]
    pep_peptides = [line.split("\t")[0] for line in pep_data]
    return (pep_header, pep_data, pep_peptides)

def CreateParser():
    """Setup the option parser"""
    parser = ArgumentParser(description="Subsample the peptides in the time series and score files for bootstrapping.  See the TPS readme for the expected file formats.")
    parser.add_argument("--firstfile", type=str, dest="firstfile", help="The path and filename of the TPS firstscores file", default=None, required=True)
    parser.add_argument("--prevfile", type=str, dest="prevfile", help="The path and filename of the TPS prevscores file", default=None, required=True)
    parser.add_argument("--tsfile", type=str, dest="tsfile", help="The path and filename of the TPS timeseries file", default=None, required=True)
    parser.add_argument("--outdir", type=str, dest="outdir", help="The path of the output directory for the subsamples files (default is the directory of the input files).", default=None, required=False)
    parser.add_argument("--fraction", type=float, dest="fraction", help="The fraction of peptides to keep in the subsampled datasets (default 0.9).", default=0.9, required=False)    
    parser.add_argument("--copies", type=int, dest="copies", help="The number of subsampled copies to generate (default 10).", default=10, required=False)
    parser.add_argument("--seed", type=int, dest="seed", help="A seed for the pseudo-random number generator for reproducibility.", default=None, required=False)
    return parser


if __name__ == "__main__":
    """Use the command line arguments to setup the options
    (the same as the default ArgumentParser behavior)
    """
    Main(sys.argv[1:])
