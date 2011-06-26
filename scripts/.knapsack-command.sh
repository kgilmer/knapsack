
if [ -z $KNAPSACK_PORT ]; then
	echo "KNAPSACK_PORT must be set."
	exit 1
fi

echo "`basename $0` $@" | nc localhost $KNAPSACK_PORT
