#!/bin/sh
# Knapsack shell script - List Services

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

echo "org.knapsack.bundle=false" > $CONTROL
echo "org.knapsack.service=true" > $CONTROL
echo "org.knapsack.property=false" > $CONTROL
echo "org.knapsack.config=false" > $CONTROL
echo "org.knapsack.log=false" > $CONTROL
if [ "$1" == "-v" ]; then
	echo "org.knapsack.output.verbose=true" > $CONTROL
else
	echo "org.knapsack.output.verbose=false" > $CONTROL
fi

cat $INFO
