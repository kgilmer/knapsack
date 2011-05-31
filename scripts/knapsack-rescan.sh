#!/bin/sh
# Knapsack shell script - Rescan bundle directory for changes.

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

echo "rescan" > $CONTROL
