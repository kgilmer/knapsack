#!/bin/sh
# Knapsack shell script - Shutdown

if [ ! -p control ]; then
	echo "Script must be run from root of knapsack instance."
	exit 1
fi

echo "shutdown" > control
