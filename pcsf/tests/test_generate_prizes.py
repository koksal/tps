import os, sys, tempfile
import numpy as np
import pandas as pd

# Create the path to forest relative to the test_generate_prizes.py path
# Workaround due to lack of a formal Python package for the pcsf scripts
test_dir = os.path.dirname(__file__)
path = os.path.abspath(os.path.join(test_dir, ".."))
if not path in sys.path:
    sys.path.insert(1, path)

import generate_prizes as gp

class TestGeneratePrizes:
    
    def test_CalcPrize(self):
        '''
        Test the calculation of prizes from a row of p-values
        '''
        assert np.isclose(gp.CalcPrize([1.0, 0.5, 0.75, 0.001]), 3)
        assert np.isclose(gp.CalcPrize([0.003, 1.0, 0.5, 0.75, 0.01]), 2.522878)

    def test_EGFRPrizeGeneration(self):
        '''
        Test that the generated EGFR prizes match the stored EGFR prizes
        '''
        try:
            # The generated EGFR prize file
            out_prize_file = tempfile.NamedTemporaryFile(delete=False)
            out_prize_file.close()

            data_dir = os.path.join(test_dir, "..", "..", "data")
            first_file = os.path.join(data_dir, "timeseries", "p-values-first.tsv")
            prev_file = os.path.join(data_dir, "timeseries", "p-values-prev.tsv")
            map_file = os.path.join(data_dir, "timeseries", "peptide-mapping.tsv")
            args = ["--firstfile", first_file, "--prevfile", prev_file, \
                "--mapfile", map_file, "--outfile", out_prize_file.name]

            # Generate a new EGFR prize file
            gp.Main(args)
            assert os.path.isfile(out_prize_file.name)

            # Test that the generated file matches the reference version
            ref_prize_file = os.path.join(data_dir, "pcsf", "egfr-prizes.txt")
            out_prize_df = pd.read_csv(out_prize_file.name, sep="\t", names=["Protein", "Prize"])
            ref_prize_df = pd.read_csv(ref_prize_file, sep="\t", names=["Protein", "Prize"])

            # The reference prize file sorts strings with '_' differently than Python
            ref_prize_df.sort_values(by="Protein", inplace=True)
            assert out_prize_df.shape == ref_prize_df.shape, "Different output file sizes"
            assert np.all(out_prize_df["Protein"].values == ref_prize_df["Protein"].values), "Different protein names"
            assert np.all(np.isclose(out_prize_df["Prize"], ref_prize_df["Prize"])), "Different prizes"

        finally:
            # Remove temporary file here because delete=False above
            os.remove(out_prize_file.name)
