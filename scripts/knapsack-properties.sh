#!/bin/sh
# Knapsack shell script - Show Properties

if [ "$KNAPSACK_ROOT" == "" ]; then
	CONTROL=control
	INFO=info
else
	CONTROL=$KNAPSACK_ROOT/control
	INFO=$KNAPSACK_ROOT/info
fi

if [ ! -p $CONTROL ]; then
	echo "Script must be run from root of knapsack instance or KNAPSACK_ROOT environment variable must be set."
	exit 1
fi

echo "org.knapsack.bundle=false" > control
echo "org.knapsack.service=false" > control
echo "org.knapsack.property=true" > control
echo "org.knapsack.config=false" > control
echo "org.knapsack.log=false" > control
if [ "$1" == "-v" ]; then
	echo "org.knapsack.output.verbose=true" > $CONTROL
else
	echo "org.knapsack.output.verbose=false" > $CONTROL
fi

cat $INFO
