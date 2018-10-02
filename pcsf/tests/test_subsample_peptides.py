import filecmp, os, pytest, shutil, sys, tempfile

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

            ref_dir = os.path.join(test_dir, "reference_data")
            prefixes = ["p-values-first", "p-values-prev", "median-time-series"]
            for prefix in prefixes:
                original_file = os.path.join(data_dir, "timeseries", "{}.tsv".format(prefix))
                with open(original_file) as original_f:
                    original_contents = set(original_f.readlines())
                for i in range(1, 3):
                    subsampled_file = "{}-0.5-subsampled{}.tsv".format(prefix, i)
                    # Verify that the subsampled peptide files match the
                    # reference subsampled peptide files
                    assert filecmp.cmp(os.path.join(out_dir, subsampled_file), \
                        os.path.join(ref_dir, subsampled_file)), \
                        "{} does not match the reference".format(subsampled_file)

                    # Verify that the lines in the subsampled files are all
                    # in the original file
                    with open(os.path.join(out_dir, subsampled_file)) as subsampled_f:
                        subsampled_contents = set(subsampled_f.readlines())
                    assert subsampled_contents.issubset(original_contents), \
                        "{} contains lines that are not in the original file".format(subsampled_file)
        finally:
            # Remove temporary directory
            shutil.rmtree(out_dir)

    def test_InvalidArguments(self):
        '''
        Test that invalid arguments generate the expected Exceptions.
        '''
        data_dir = os.path.join(test_dir, "..", "..", "data")
        first_file = os.path.join(data_dir, "timeseries", "p-values-first.tsv")
        prev_file = os.path.join(data_dir, "timeseries", "p-values-prev.tsv")
        ts_file = os.path.join(data_dir, "timeseries", "median-time-series.tsv")
        base_args = ["--firstfile", first_file, "--prevfile", prev_file, \
            "--tsfile", ts_file]

        with pytest.raises(Exception) as excinfo:
            args = base_args
            args.extend(["--fraction", "-1"])
            sp.Main(args)
        assert "The fraction must be in (0,1)" in str(excinfo)

        with pytest.raises(Exception) as excinfo:
            args = base_args
            args.extend(["--fraction", "1.1"])
            sp.Main(args)
        assert "The fraction must be in (0,1)" in str(excinfo)

        with pytest.raises(Exception) as excinfo:
            args = base_args
            args.extend(["--fraction", "1e-20"])
            sp.Main(args)
        assert "Must increase the fraction to subsample at least one row" \
            in str(excinfo)

        with pytest.raises(Exception) as excinfo:
            args = base_args
            args.extend(["--fraction", "0.9999999"])
            sp.Main(args)
        assert "Must decrease the fraction to subsample less than the total number of rows" \
            in str(excinfo)

        with pytest.raises(Exception) as excinfo:
            args = base_args
            args.extend(["--copies", "0"])
            sp.Main(args)
        assert "The number of copies must be positive" in str(excinfo)
