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

    # TODO: add prize calculation, write prize file

def LoadPeptideMap(mapfile):
    """Parse the peptide-to-protein map file.  The peptide is in the first column
    and the protein or proteins are in the second.  The file contains no header row.
    A peptide may map to a set of pipe-delimited proteins.  A peptide may also
    appear multiple times, and the map will include all proteins mapped to that
    peptide in any line.
    """
    pep_prot_map = defaultdict(set)
    unique_prots = set()
    
    with open(mapfile) as map_f:
        for line in map_f:
            parts = line.strip().split('\t')
            assert len(parts) == 2, 'Expected tab-delimited peptide and protein (or pipe-delimited proteins) on each line'
            
            pep = parts[0]
            prots = parts[1].split('|')
            
            # Add all proteins that map to the peptide in this line to any
            # existing proteins that mapped to the peptide in previous lines
            pep_prot_map[pep].update(prots)
            # Add the proteins to the set of all unique observed proteins
            unique_prots.update(prots)
    
    print 'Loaded {} unique peptides that map to {} unique proteins'.format(len(pep_prot_map), len(unique_prots))
    return pep_prot_map
    

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