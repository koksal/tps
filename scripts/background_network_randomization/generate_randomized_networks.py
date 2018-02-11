from argparse import ArgumentParser
import os
import pandas
import subprocess
import tempfile

def run(args):
    parser = setup_parser()
    options = parser.parse_args(arg_list)

    input_data = parse_file(options.network)
    undirected_subgraph, directed_subgraph = partition_edges(input_data)

    randomized_ug = randomize_undirected(undirected_subgraph)
    randomized_dg = randomize_directed(directed_subgraph)

    add_randomized_weights(undirected_subgraph, randomized_ug)
    add_randomized_weights(directed_subgraph, randomized_dg)

    add_orientation_info(randomized_ug, 'U')
    add_orientation_info(randomized_dg, 'D')

    save_result(randomized_ug, randomized_dg)

    os.remove(randomized_ug)
    os.remove(randomized_dg)

def parse_file(input_file):
    return pandas.read_csv(input_file, sep = "\t")

def partition_edges(input_data):
    undirected_subgraph = input_data[input_data.orientation == 'U']
    directed_subgraph = input_data[input_data.orientation == 'D']
    return undirected_subgraph, directed_subgraph

def randomize_undirected(undirected_subgraph):
    input_file = prepare_undirected_input(undirected_subgraph)
    output_file = make_temp_file()

    subprocess.check_call([
        'Rscript',
        'scripts/background_network_randomization/randomizeUndirectedNetwork.R', 
        input_file, 
        output_file])

    randomized_graph = parse_file(output_file)

    os.remove(input_file)
    os.remove(output_file)

    return randomized_graph

def prepare_undirected_input(undirected_subgraph):
    # save an (undirected) edgelist for igraph
    undirected_edgelist = undirected_subgraph[['id1', 'id2']]
    input_filename = make_temp_file()
    undirected_edgelist.to_csv(input_filename, sep = ' ', header = False, index = False)
    return input_filename

def randomize_directed(directed_subgraph):
    input_file = prepare_directed_input(directed_subgraph)
    output_file = make_temp_file()
    subprocess.check_call([
        'Rscript',
        'scripts/background_network_randomization/randomizeDirectedNetwork.R', 
        input_file, 
        output_file])

    randomized_graph = parse_file(output_file)

    os.remove(input_file)
    os.remove(output_file)

    return randomized_graph

def prepare_directed_input(directed_subgraph):
    # save SIF for BiRewire
    directed_edgelist = directed_subgraph[['id1', 'orientation', 'id2']]
    input_filename = make_temp_file()
    directed_edgelist.to_csv(input_filename, sep = ' ', header = False, index = False)
    return input_filename

def add_randomized_weights(original_graph, randomized_graph):
    nb_rand_edges = randomized_graph.shape[0]
    sampled_weights = original_graph['weight'].sample(n = nb_rand_edges, replace = True)
    randomized_graph['weight'] = sampled_weights

def add_orientation_info(df, orientation):
    df['orientation'] = orientation

def save_result(undirected_graph, directed_graph):
    full_output = pandas.concat([undirected_graph, directed_graph])
    full_output.to_csv(output_file, sep = '\t', header = True, index = False)

def make_temp_file():
    _, fn = tempfile.mkstemp()
    return fn

def setup_parser():
    parser = ArgumentParser(description="Randomize background network.")
    parser.add_argument("--network", type=str, dest="network", help="Background network file in tsv format.", default=None, required=True)
    parser.add_argument("--outdir", type=str, dest="outdir", help="Output directory for randomized files", default=None, required=False)
    parser.add_argument("--copies", type=int, dest="copies", help="The number of subsampled copies to generate (default 10).", default=10, required=False)
    return parser

if __name__ == '__main__':
    run(sys.argv[1:])
