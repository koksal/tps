import glob, os, sys, tempfile

# Create the path to forest relative to the test_generate_prizes.py path
# Workaround due to lack of a formal Python package for the pcsf scripts
test_dir = os.path.dirname(__file__)
path = os.path.abspath(os.path.join(test_dir, ".."))
if not path in sys.path:
    sys.path.insert(1, path)

import summarize_sif as ss

def files_match(generated_file, reference_file):
    '''
    Compare two files after sorting the lines in order to be insensitive
    to the order in which dictionaries were iterated over when writing
    the file
    '''
    with open(generated_file) as gen, open(reference_file) as ref:
        generated_contents = sorted(gen.readlines())
        print(generated_contents)
        reference_contents = sorted(ref.readlines())
        print(reference_contents)
        return generated_contents == reference_contents

class TestSummarizeSif:

    data_dir = os.path.join(test_dir, "reference_data")

    def test_LoadSifNetwork(self):
        '''
        Test loading a sif-formatted network file
        '''
        toy_graph_file = os.path.join(self.data_dir, "toy_graph_0.sif")
        graph = ss.LoadSifNetwork(toy_graph_file)

        # Check that the graph has the expected size and that the duplciate
        # edge is ignored
        assert graph.order() == 4, "Unexpected number of nodes"
        assert graph.size() == 4, "Unexpected number of edges"
        
        # Check that the graph has the expected edges
        assert graph.has_edge('A','B')
        assert graph.has_edge('B','C')
        assert graph.has_edge('B','D')
        assert graph.has_edge('C','D')

    def test_SummarizeSif(self):
        '''
        Test summarizing four toy graphs
        '''
        try:
            # The generated summary file prefix
            out_file_base = tempfile.NamedTemporaryFile(delete=False)
            out_file_base.close()

            # Summarize the toy graphs
            args = ["--indir", self.data_dir, "--pattern", "toy_graph_*.sif", \
                "--outfile", out_file_base.name]
            ss.Main(args)
            
            # Confirm output files exist and match the reference version,
            # ignoring the order of the lines in the file
            # The file comparison may be too strict if different versions of
            # Python write out a different number of decimal places, in which
            # case the floating point numbers should be parsed and compared
            out_size_file = out_file_base.name + "_size.txt"
            assert files_match(out_size_file, os.path.join(self.data_dir, "toy_size.txt")), \
                "Output size file does not match the reference"
            
            out_union_tsv_file = out_file_base.name + "_union.tsv"
            assert files_match(out_union_tsv_file, os.path.join(self.data_dir, "toy_union.tsv")), \
                "Output union file does not match the reference"
            
            out_union_sif_file = out_file_base.name + "_union.sif"
            assert files_match(out_union_sif_file, os.path.join(self.data_dir, "toy_union.sif")), \
                "Output union file does not match the reference"
            
            out_node_file = out_file_base.name + "_nodeAnnotation.txt"
            assert files_match(out_node_file, os.path.join(self.data_dir, "toy_nodeAnnotation.txt")), \
                "Output node annotation file does not match the reference"
            
            out_edge_file = out_file_base.name + "_edgeAnnotation.txt"
            assert files_match(out_edge_file, os.path.join(self.data_dir, "toy_edgeAnnotation.txt")), \
                "Output edge annotation file does not match the reference"

        finally:
            # Remove temporary files here because delete=False above
            for out_file in glob.glob(out_file_base.name + "*"):
                os.remove(out_file)
