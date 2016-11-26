import os, sys, glob, csv, itertools
import networkx as nx
from argparse import ArgumentParser

__author__ = "Anthony Gitter"

def Main(argList):
    """Parse the arguments, which either come from the command line or a list
    provided by the Python code calling this function
    """
    parser = CreateParser()
    options = parser.parse_args(argList)

    if options.indir is None or options.outfile is None:
        raise RuntimeError("Must specify --indir and --outfile")

    if options.pattern is None and options.siflist is None:
        raise RuntimeError("Must specify --pattern or --siflist")

    # Load the common set of prizes if provided
    prizeMap = dict()
    if not options.prizefile is None:
        prizeMap = LoadPrizes(options.prizefile)
        print "%d prizes loaded" % len(prizeMap)
    # The proteins with prizes
    prizes = set(prizeMap.keys())

    # Read and load the information about each forest
    names = []
    forests = []
    forestNodes = []
    forestEdges = []
    
    if not options.pattern is None:
        pattern = os.path.join(options.indir,options.pattern)
        sifFiles = glob.glob(pattern)
    else:
        sifFiles = [os.path.join(options.indir, f) for f in options.siflist.split("|")]

    for sifFile in sifFiles:
        names.append(os.path.basename(sifFile))
        curForest = LoadSifNetwork(sifFile)
        forests.append(curForest)
        forestNodes.append(set(curForest.nodes()))
        # Sort the nodes in each edge tuple because we treat them as
        # undirected edges
        forestEdges.append(set(map(SortEdge, curForest.edges())))

    print "%d forests loaded" % len(names)
    
    if len(names) == 0:
        raise RuntimeError("Must provide 1 or more forests as input")

    # Store the Steiner nodes, which are the forest nodes that are not prizes
    steinerNodes = []
    for i in range(len(forestNodes)):
        steinerNodes.append(forestNodes[i].difference(prizes))
        
    # Store the degree of the hub node of interest
    if not options.hubnode is None:
        # The degree is 0 if the hub node is not in the forest
        hubDegrees = [forest.degree(options.hubnode) if options.hubnode in forest else 0 for forest in forests]

    # Write the sizes of each forest
    # Could use pandas for this to build the table in memory instead of
    # writing to a file and transposing
    emptyCount = 0
    tmpFile = options.outfile + "_size_tmp.txt"
    with open(tmpFile, "w") as f:
        f.write("Forest name")
        for name in names:
            f.write("\t" + name)
        f.write("\n")
        f.write("Forest size")
        for forest in forestNodes:
            f.write("\t%d" % len(forest))
            if(len(forest) == 0):
                emptyCount += 1
        f.write("\n")
        # Only write Steiner nodes and prizes if prizes were loaded
        if len(prizes) > 0:
            f.write("Steiner nodes")
            for steiner in steinerNodes:
                f.write("\t%d" % len(steiner))
            f.write("\n")
            f.write("Prizes in forest")
            for i in range(len(forestNodes)):
                f.write("\t%d" % (len(forestNodes[i])-len(steinerNodes[i])))
            f.write("\n")
            f.write("Total prizes")
            for i in range(len(forestNodes)):
                f.write("\t%d" % len(prizes))
            f.write("\n")
        # Only write hub node degrees and the ratio
        # of hub node degree to forest size if a hub was specified
        if not options.hubnode is None:
            f.write("%s degree" % options.hubnode)
            for degree in hubDegrees:
                f.write("\t%d" % degree)
            f.write("\n")
            f.write("%s degree / forest size" % options.hubnode)
            for i in range(len(forestNodes)):
                if (len(forestNodes[i]) == 0):
                    f.write("\t0")
                else:
                    f.write("\t%f" % (float(hubDegrees[i]) / len(forestNodes[i])))
            f.write("\n")
            
    print "%d empty forests" % emptyCount
    
    # See http://stackoverflow.com/questions/4869189/how-to-pivot-data-in-a-csv-file
    # Transpose the file so that rows become columns
    # The reader iterator iterates over rows and izip is used to create new tuples
    # from the ith element in each row
    with open(tmpFile, "rb") as beforeTransFile, open(options.outfile + "_size.txt", "wb") as afterTransFile:
        transposed = itertools.izip(*csv.reader(beforeTransFile, delimiter = "\t"))
        csv.writer(afterTransFile, delimiter = "\t").writerows(transposed)
    # Remove the temporary file
    os.remove(tmpFile)

    # Write the union network in the TPS tab-delimited format
    # Edge directions are not recorded and must be specified in
    # the TPS partial model (prior knowledge) file
    edgeFreqDict = SetFrequency(forestEdges)
    with open(options.outfile + "_union.tsv", "w") as unionFile:
        for (edge, freq) in edgeFreqDict.iteritems():
            unionFile.write("%s\t%s\n" % (edge[0], edge[1]))

    # Write the node and edge annotation files and union network in the
    # Cytoscape format
    if not options.cyto28:
        # Write a Cytoscape attribute table file for the forest node frequency
        with open(options.outfile + "_nodeAnnotation.txt", "w") as f:
            f.write("Protein\tNodeFreq\tPrize\n")
            for (node, freq) in SetFrequency(forestNodes).iteritems():
                f.write("%s\t%f\t%s\n" % (node, freq, prizeMap.setdefault(node, "")))
    
        # Write a Cytoscape attribute table file for the forest edge frequency and a sif file for the union of
        # all forests
        with open(options.outfile + "_edgeAnnotation.txt", "w") as edaFile:
            edaFile.write("Interaction\tEdgeFreq\n")
            with open(options.outfile + "_union.sif", "w") as sifFile:
                for (edge, freq) in edgeFreqDict.iteritems():
                    edaFile.write("%s (pp) %s\t%f\n" % (edge[0], edge[1], freq))
                    sifFile.write("%s pp %s\n" % (edge[0], edge[1]))
    else:
        # Write a Cytoscape .noa file for the forest node frequency
        with open(options.outfile + "_nodeFreq.noa", "w") as f:
            f.write("NodeFrequency\n")
            for (node, freq) in SetFrequency(forestNodes).iteritems():
                f.write("%s = %f\n" % (node, freq))
    
        # Write a Cytoscape .eda file for the forest edge frequency and a sif file for the union of
        # all forests
        with open(options.outfile + "_edgeFreq.eda", "w") as edaFile:
            with open(options.outfile + "_union.sif", "w") as sifFile:
                edaFile.write("EdgeFrequency\n")
                for (edge, freq) in edgeFreqDict.iteritems():
                    edaFile.write("%s (pp) %s = %f\n" % (edge[0], edge[1], freq))
                    sifFile.write("%s pp %s\n" % (edge[0], edge[1]))


def LoadSifNetwork(networkFile):
    """Load an interaction network as an undirected graph from a sif format
    edge list.  Only the first and third columns, the node names, are used.  Edge
    attributes are ignored.  Multiple instances of the same edge are collapsed
    and self edges are ignored.  Edge directions are ignored.  If the input
    network to PCSF had directed edges, that directionality should be provided
    as prior knowledge to TPS.
    
    Edges have the format "node1 edgeType node2"
    """
    graph = nx.Graph()
    with open(networkFile) as f:
        # Read all edges in this network and build a graph
        # If an edge is listed twice it will only be stored once because this is not a MultiGraph
        for edgeLine in f:
            edgeParts = edgeLine.split()
            # Ignore self edges
            if not edgeParts[0] == edgeParts[2]:
                graph.add_edge(edgeParts[0], edgeParts[2])
    return graph


# This is not written for maximum efficiency but rather readability.
def SetCounts(setList):
    """Take a list of sets and return a dict that maps elements in the sets
    to the number of sets they appear in as ints.
    """
    keys = set()
    for curSet in setList:
        keys.update(curSet)

    # Initialize the dictionary that will be used to store the counts of each element
    countDict = dict.fromkeys(keys, 0)
    # Populate the counts
    for curSet in setList:
        for element in curSet:
            countDict[element] += 1

    return countDict

# This is not written for maximum efficiency but rather readability.
def SetFrequency(setList):
    """Take a list of sets and return a dict that maps elements in the sets
    to the fraction of sets they appear in.
    """
    n = float(len(setList)) # Want floating point division
    countDict = SetCounts(setList)

    # Transform the counts into frequencies
    freqDict = {}
    for key in countDict.keys():
        freqDict[key] = countDict[key] / n

    return freqDict


def SortEdge(edge):
    """Sort a pair of nodes alphabetically"""
    return tuple(sorted(edge))


def LoadPrizes(prizeFile):
    """Load the mapping of all nodes to their prizes.
    Prizes are not cast as floats but left as strings.
    """
    prizes = dict()
    with open(prizeFile) as f:
        for line in f:
            parts = line.split()
            prizes[parts[0]] = parts[1]
    return prizes


def CreateParser():
    """Setup the option parser"""
    parser = ArgumentParser(description="Summarize a collection of Steiner forests")
    parser.add_argument("--indir", type=str, dest="indir", help="The path to the directory that contains sif files.", default=None, required=True)
    parser.add_argument("--pattern", type=str, dest="pattern", help="The filename pattern of the sif files in indir.  Not needed if a siflist is provided instead", default=None)
    parser.add_argument("--siflist", type=str, dest="siflist", help="A list of sif files in indir delimited by '|'.  Not used if a pattern is provided.", default=None)
    parser.add_argument("--prizefile", type=str, dest="prizefile", help="The path and filename prefix of the prize file (optional).  Assumes the same prize file was used for all forests.", default=None)
    parser.add_argument("--outfile", type=str, dest="outfile", help="The path and filename prefix of the output.  Does not include an extension.", default=None, required=True)
    parser.add_argument("--hubnode", type=str, dest="hubnode", help="The name of a hub node in the network (optional).  The degree of this node will be reported.", default=None)    
    parser.add_argument("--cyto28", action="store_true", dest="cyto28", help="This flag will generate node and edge frequency annotation files in the Cytoscape 2.8 format instead of the default Cytoscape 3 style.", default=False)
    return parser


if __name__ == "__main__":
    """Use the command line arguments to setup the options
    (the same as the default ArgumentParser behavior)
    """
    Main(sys.argv[1:])
