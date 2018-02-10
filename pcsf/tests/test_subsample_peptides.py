import filecmp, os, shutil, sys, tempfile

# Create the path to forest relative to the test_subsample_peptides.py path
# Workaround due to lack of a formal Python package for the pcsf scripts
test_dir = os.path.dirname(__file__)
path = os.path.abspath(os.path.join(test_dir, ".."))
if not path in sys.path:
    sys.path.insert(1, path)

import subsample_peptides as sp

class TestSubsamplePeptides:

    def test_EGFRSubsample(self):
        '''
        Test that the subsampled EGFR peptide data match the reference
        subsampled data.
        '''
        try:
            # Temporary directory for the subsampled peptide files
            out_dir = tempfile.mkdtemp()

            # Generate subsampled peptide files
            data_dir = os.path.join(test_dir, "..", "..", "data")
            first_file = os.path.join(data_dir, "timeseries", "p-values-first.tsv")
            prev_file = os.path.join(data_dir, "timeseries", "p-values-prev.tsv")
            ts_file = os.path.join(data_dir, "timeseries", "median-time-series.tsv")
            args = ["--firstfile", first_file, "--prevfile", prev_file, \
                "--tsfile", ts_file, "--outdir", out_dir, "--fraction", "0.5", \
                "--copies", "2", "--seed", "100"]
            sp.Main(args)

            # Verify that the subsampled peptide files match the reference
            # files
            ref_dir = os.path.join(test_dir, "reference_data")
            prefixes = ["p-values-first", "p-values-prev", "median-time-series"]
            for i in range(1, 3):
                for prefix in prefixes:
                    subsampled_file = "{}-0.5-subsampled{}.tsv".format(prefix, i)
                    assert filecmp.cmp(os.path.join(out_dir, subsampled_file), \
                        os.path.join(ref_dir, subsampled_file)), \
                        "{} does not match the reference".format(subsampled_file)
        finally:
            # Remove temporary directory
            shutil.rmtree(out_dir)
