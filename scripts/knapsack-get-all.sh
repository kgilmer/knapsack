#!/bin/sh
# Knapsack shell script - Print all info

if [ ! -p control ]; then
	echo "Script must be run from root of knapsack instance."
	exit 1
fi

echo "org.knapsack.bundle=true" > control
echo "org.knapsack.service=true" > control
echo "org.knapsack.property=true" > control
echo "org.knapsack.config=true" > control
echo "org.knapsack.log=true" > control
echo "org.knapsack.output.verbose=true" > control

cat info
