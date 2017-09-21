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

            # Test that the individual files are loaded correctly
            pep_prot_map = gp.LoadPeptideMap(map_file)
            assert len(pep_prot_map) == 2917, "Unexpected number of peptides"
            unique_proteins = set()
            for proteins in pep_prot_map.values():
                unique_proteins.update(proteins)           
            assert len(unique_proteins) == 1555, "Unexpected number of proteins"

            merged_scores_df = gp.LoadScores(first_file, prev_file)
            assert merged_scores_df.shape == (1068, 15), "Unexpected merged scores"
            assert np.isclose(merged_scores_df.loc["K.n[305.21]AY[243.03]HEQLSVAEITNAC[160.03]FEPANQMVK[432.30].C", "prize"], \
                5.367872), "Unexpected peptide prize"
            assert np.isclose(merged_scores_df.loc["R.n[305.21]YSEEGLS[167.00]PSK[432.30].R", "prize"], \
                1.358383), "Unexpected peptide prize"

            # Generate a new EGFR prize file
            gp.Main(args)
            assert os.path.isfile(out_prize_file.name), "Prize file was not written"

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
