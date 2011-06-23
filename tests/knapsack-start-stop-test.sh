#!/usr/bin/env roundup

describe "Knapsack Start/Stop Tests"

it_should_be_startable() {
	java -jar knapsack.jar &
	sleep 1
}

it_should_create_info_pipe() {
	file info
}

it_should_create_control_pipe() {
	file control
}

it_should_create_scripts_dir() {
	file scripts
	file scripts/ks-shutdown.sh
}

it_should_shutdown() {
	scripts/ks-shutdown.sh
	sleep 1
	test ! -e info
	test ! -e control
}