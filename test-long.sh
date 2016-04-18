#!/bin/bash
echo this takes about 20 minutes and tests bufferOnFile more intensively than
echo a standard build.
mvn clean install -Dmax.small=100000000 -Dmax.medium=300000 -Dmax.seconds=600 -Dloops=10000


