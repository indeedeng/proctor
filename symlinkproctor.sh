#!/bin/sh
mkdir  ~/.gradle/local-repo/maven/com
mkdir  ~/.gradle/local-repo/maven/com/indeed
ln -s ~/.m2/repository/com/indeed/proctor*  ~/.gradle/local-repo/maven/com/indeed || echo "Link already exists"