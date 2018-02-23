from argparse import ArgumentParser
import os, sys, subprocess, tempfile
import pandas

def run(args):
    parser = setup_parser()
    options = parser.parse_args(args)

    input_data = parse_file(options.network)
    undirected_subgraph, directed_subgraph = partition_edges(input_data)

    for i in range(1, options.copies + 1):
        randomized_ug, randomized_dg = randomize(undirected_subgraph, directed_subgraph)
        merged_network = merge_result(randomized_ug, randomized_dg)
        merged_file, partial_model_file = make_output_filenames(options.network, options.outdir, i)

        merged_network.to_csv(merged_file, sep = '\t', header = True, index = False)
        save_partial_model(randomized_dg, partial_model_file)

def randomize(undirected_subgraph, directed_subgraph):
    randomized_ug = randomize_undirected(undirected_subgraph)
    randomized_dg = randomize_directed(directed_subgraph)

    add_randomized_weights(undirected_subgraph, randomized_ug)
    add_randomized_weights(directed_subgraph, randomized_dg)

    add_orientation_info(randomized_ug, 'U')
    add_orientation_info(randomized_dg, 'D')

    return randomized_ug, randomized_dg

def make_output_filenames(input_file, outdir, i):
    input_prefix, extension = os.path.splitext(input_file)
    output_prefix = input_prefix
    if outdir:
        input_parent, input_file_prefix = os.path.split(input_prefix)
        output_prefix = os.path.join(os.path.normpath(outdir), input_file_prefix)

    network_file = "{}-randomized{}{}".format(output_prefix, i, extension)
    partial_model_file = "{}-partial-model-randomized{}.sif".format(output_prefix, i)

    return network_file, partial_model_file

def save_partial_model(network, filename):
    network_in_sif = network[['id1', 'orientation', 'id2']]
    network_in_sif['orientation'] = 'N'
    network_in_sif.to_csv(filename, sep = '\t', header = False, index = False)

def parse_file(input_file):
    return pandas.read_csv(input_file, sep = "\t")

def parse_undirected_output(f):
    return pandas.read_csv(f, sep = ' ', header = None,
            names = ['id1', 'id2'])

def parse_directed_output(f):
    data = pandas.read_csv(f, sep = ' ', header = None, 
            names = ['id1', 'orientation', 'id2'])
    # remove bogus negative edges introduced to make BiRewire work.
    data = data[data.orientation == '+']
    return data[['id1', 'id2']]

def partition_edges(input_data):
    undirected_subgraph = input_data[input_data.orientation == 'U']
    directed_subgraph = input_data[input_data.orientation == 'D']
    return undirected_subgraph, directed_subgraph

def randomize_undirected(undirected_subgraph):
    input_file = prepare_undirected_input(undirected_subgraph)
    output_file = make_temp_file('undirected_birewire_output')

    subprocess.check_call([
        'Rscript',
        'scripts/background_network_randomization/randomizeUndirectedNetwork.R', 
        input_file, 
        output_file])

    randomized_graph = parse_undirected_output(output_file)

    os.remove(input_file)
    os.remove(output_file)

    return randomized_graph

def prepare_undirected_input(undirected_subgraph):
    # save an (undirected) edgelist for igraph
    undirected_edgelist = undirected_subgraph[['id1', 'id2']]
    input_filename = make_temp_file('undirected_birewire_input')
    undirected_edgelist.to_csv(input_filename, sep = ' ', header = False, index = False)
    return input_filename

def randomize_directed(directed_subgraph):
    input_file = prepare_directed_input(directed_subgraph)
    output_file = make_temp_file('directed_birewire_output')
    subprocess.check_call([
        'Rscript',
        'scripts/background_network_randomization/randomizeDirectedNetwork.R', 
        input_file, 
        output_file])

    randomized_graph = parse_directed_output(output_file)

    os.remove(input_file)
    os.remove(output_file)

    return randomized_graph

def prepare_directed_input(directed_subgraph):
    # save SIF for BiRewire and add pseudo negative edges to make it work
    cols = ['id1', 'orientation', 'id2']
    positive_edgelist = directed_subgraph[cols]
    positive_edgelist['orientation'] = '+'
    pseudo_negative_edgelist = pandas.DataFrame(
            [
                ["PSEUDO_NEG1", "-", "PSEUDO_NEG2"], 
                ["PSEUDO_NEG3", "-", "PSEUDO_NEG4"]
            ], 
            columns = cols)
    edgelist = positive_edgelist.append(pseudo_negative_edgelist)

    input_filename = make_temp_file('directed_birewire_input')
    edgelist.to_csv(input_filename, sep = ' ', header = False, index = False)
    return input_filename

def add_randomized_weights(original_graph, randomized_graph):
    nb_rand_edges = randomized_graph.shape[0]
    sampled_weights = original_graph['weight'].sample(n = nb_rand_edges, replace
            = False).reset_index(drop=True)
    randomized_graph['weight'] = sampled_weights

def add_orientation_info(df, orientation):
    df['orientation'] = orientation

def merge_result(undirected_graph, directed_graph):
    return pandas.concat([undirected_graph, directed_graph])

def make_temp_file(label):
    _, fn = tempfile.mkstemp(suffix = label, dir = '.')
    return fn

def setup_parser():
    parser = ArgumentParser(description="Randomize background network.")
    parser.add_argument("--network", type=str, dest="network", help="Background network file in tsv format.", default=None, required=True)
    parser.add_argument("--outdir", type=str, dest="outdir", help="Output directory for randomized files", default=None, required=False)
    parser.add_argument("--copies", type=int, dest="copies", help="The number of subsampled copies to generate (default 10).", default=10, required=False)
    return parser

if __name__ == '__main__':
    run(sys.argv[1:])
