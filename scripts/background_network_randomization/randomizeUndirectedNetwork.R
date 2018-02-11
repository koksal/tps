library(BiRewire)
library(igraph)

args = commandArgs(trailingOnly = TRUE)
inputFile = args[[1]]
outputFile = args[[2]]

inputEdges = read.table(inputFile, sep = ' ')
flattenedRowMajorData = as.vector(t(inputEdges))
inputGraph = make_undirected_graph(flattenedRowMajorData)

randomizedGraph = birewire.rewire.undirected(inputGraph)

write_graph(randomizedGraph, outputFile, format = 'ncol')
