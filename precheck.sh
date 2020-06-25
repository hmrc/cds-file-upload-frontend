#!/usr/bin/env bash

sbt clean test scalafmt::test test:scalafmt::test
