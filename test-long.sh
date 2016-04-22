#!/bin/bash
echo ---------------------------------------------
echo this takes about 35 minutes and tests 
echo Transformer.onBackpressureBufferToFile more 
echo intensively than a standard build.
echo ---------------------------------------------
mvn clean install -Dmax.small=100000000 -Dmax.medium=300000 -Dmax.seconds=600 -Dloops=10000


