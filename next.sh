#!/usr/bin/env bash
git reset --hard origin/master
git checkout restdocs-documented
./gradlew clean
