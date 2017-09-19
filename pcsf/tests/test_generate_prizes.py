import os, sys
import numpy as np

# Create the path to forest relative to the test_generate_prizes.py path
# Workaround due to lack of a formal Python package for the pcsf scripts
path = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if not path in sys.path:
    sys.path.insert(1, path)

import generate_prizes as gp

class TestGeneratePrizes:
	
	def test_CalcPrize(self):
		assert np.isclose(gp.CalcPrize([1.0, 0.5, 0.75, 0.001]), 3)
