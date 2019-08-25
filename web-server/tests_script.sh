#!/bin/bash

time java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -s BFS -w 1024 -h 1024 -i datasets/RANDOM_HILL_1024x1024_2019-03-08_17-00-23.dat -o output -yS 512 -xS 512 | grep real

time java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -s DFS -w 1024 -h 1024 -i datasets/RANDOM_HILL_1024x1024_2019-03-08_17-00-23.dat -o output -yS 512 -xS 512 | grep real

time java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -s ASTAR -w 1024 -h 1024 -i datasets/RANDOM_HILL_1024x1024_2019-03-08_17-00-23.dat -o output -yS 512 -xS 512 | grep real

time java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -s BFS -w 512 -h 512 -i datasets/RANDOM_HILL_1024x1024_2019-03-08_17-00-23.dat -o output -yS 256 -xS 256 | grep real

time java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -s DFS -w 512 -h 512 -i datasets/RANDOM_HILL_1024x1024_2019-03-08_17-00-23.dat -o output -yS 256 -xS 256 | grep real

time java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -s ASTAR -w 512 -h 512 -i datasets/RANDOM_HILL_1024x1024_2019-03-08_17-00-23.dat -o output -yS 256 -xS 256 | grep real

time java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -s BFS -w 256 -h 256 -i datasets/RANDOM_HILL_1024x1024_2019-03-08_17-00-23.dat -o output -yS 128 -xS 128 | grep real

time java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -s DFS -w 256 -h 256 -i datasets/RANDOM_HILL_1024x1024_2019-03-08_17-00-23.dat -o output -yS 128 -xS 128 | grep real

time java pt.ulisboa.tecnico.cnv.solver.SolverMain -d -s ASTAR -w 256 -h 256 -i datasets/RANDOM_HILL_1024x1024_2019-03-08_17-00-23.dat -o output -yS 128 -xS 128 | grep real