#!/bin/sh
# Knapsack shell script - Print all info
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

echo "org.knapsack.bundle=true" > $CONTROL
echo "org.knapsack.service=true" > $CONTROL
echo "org.knapsack.property=true" > $CONTROL
echo "org.knapsack.config=true" > $CONTROL
echo "org.knapsack.log=true" > $CONTROL
echo "org.knapsack.output.verbose=true" > $CONTROL

cat $INFO
