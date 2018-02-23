library(BiRewire)

args = commandArgs(trailingOnly = TRUE)
inputFile = args[[1]]
outputFile = args[[2]]

dsg = birewire.load.dsg(inputFile)
bipartite = birewire.induced.bipartite(dsg)

randomized = birewire.rewire.dsg(bipartite, path = outputFile)
