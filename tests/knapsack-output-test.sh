#!/usr/bin/env roundup

describe "Knapsack Output Tests"

it_should_have_bundle_output() {
	echo "starting knapsack"
	java -jar knapsack.jar &
	sleep 2
	
	scripts/ks-bundles.sh -v > bundle.out
	test -s bundle.out
	
	echo "stopping knapsack"
	scripts/ks-shutdown.sh
	sleep 2
}

it_should_have_service_output() {
	echo "starting knapsack"
	java -jar knapsack.jar &
	sleep 2
	
	scripts/ks-services.sh -v > service.out
	test -s service.out
	
	echo "stopping knapsack"
	scripts/ks-shutdown.sh
	sleep 2
}
