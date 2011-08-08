#!/bin/bash

TTL=1000

lein run "$1" Philosophy $TTL | tee "$1"-Philosophy-$TTL
