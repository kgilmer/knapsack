#!/bin/bash
if [ -z $KNAPSACK_PORT ]; then
	KNAPSACK_PORT=8892
fi

nc -z localhost $KNAPSACK_PORT

if [ "$?" -ne "0" ]; then
	echo "Unable to connect to Knapsack's socket: $KNAPSACK_PORT"
	exit 1
else
	echo "`basename $0` $@" | nc localhost $KNAPSACK_PORT
fi
