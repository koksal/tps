import filecmp, os, shutil, sys, tempfile

# Create the path to forest relative to the test_permute_proteins.py path
# Workaround due to lack of a formal Python package for the pcsf scripts
test_dir = os.path.dirname(__file__)
path = os.path.abspath(os.path.join(test_dir, ".."))
if not path in sys.path:
    sys.path.insert(1, path)

import permute_proteins as pp

class TestPermuteProteins:

    def test_EGFRPermutation(self):
        '''
        Test that the permuted EGFR peptide maps match the reference permuted
        maps
        '''
        try:
            # Temporary directory for permuted peptide map files
            out_dir = tempfile.mkdtemp()

            # Generate permuted peptide-protein maps
            data_dir = os.path.join(test_dir, "..", "..", "data")
            map_file = os.path.join(data_dir, "timeseries", "peptide-mapping.tsv")
            args = ["--mapfile", map_file, "--outdir", out_dir, \
                "--copies", "2", "--seed", "100"]
            pp.Main(args)

            # Verify that the permuted peptide map files match the reference
            # files
            ref_dir = os.path.join(test_dir, "reference_data")
            for i in range(1, 3):
                permuted_file = "peptide-mapping-shuffled{}.tsv".format(i)
                assert filecmp.cmp(os.path.join(out_dir, permuted_file), \
                    os.path.join(ref_dir, permuted_file)), \
                    "{} does not match the reference".format(permuted_file)
        finally:
            # Remove temporary directory
            shutil.rmtree(out_dir)
