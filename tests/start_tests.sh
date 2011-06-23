#!/bin/bash
# This script is for testing knapsack.  
# Since knapsack is designed to be used from the shell, the tests are also composed for the shell.

if [ ! -d roundup ]; then
	git clone git://github.com/bmizerany/roundup.git
	cd roundup
	./configure
	make
	cd ..
fi

roundup/roundup
