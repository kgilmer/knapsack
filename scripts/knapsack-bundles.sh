#!/bin/sh
# Knapsack shell script - Print bundle info

if [ ! -p control ]; then
	echo "Script must be run from root of knapsack instance."
	exit 1
fi

echo "org.knapsack.bundle=true" > control
echo "org.knapsack.service=false" > control
echo "org.knapsack.property=false" > control
echo "org.knapsack.config=false" > control
echo "org.knapsack.log=false" > control
echo "org.knapsack.output.verbose=false" > control

cat info
