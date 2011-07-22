#!/bin/sh
# .knapsack-command.sh
#
# This is the internal shell command used to pass 'native' commands to knapsack.
# It simply forwards the entire command line to knapsack, waits for a response, 
# And prints the response back to the user.

if [ -z $KNAPSACK_PORT ]; then
	echo "KNAPSACK_PORT must be set."
	exit 1
fi

echo "`basename $0` $@" | nc -q 1 127.0.0.1 $KNAPSACK_PORT
