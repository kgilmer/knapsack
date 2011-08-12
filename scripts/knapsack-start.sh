#!/bin/sh
# This script allows for the start of a knapsack-based Felix session from the command line.
# It requires that the knapsack.jar file be on the path and have the executable bit set.

KNAPSACK_JAR=`which knapsack.jar`

if [ ! -n "$KNAPSACK_JAR" ]; then
	echo "Unable to find knapsack.jar, please check that it is in the path and executable."
	exit 1
fi

java -jar $KNAPSACK_JAR $@ &
