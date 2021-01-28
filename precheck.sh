#!/usr/bin/env bash

sbt clean scalafmt test:scalafmt test scalafmt::test test:scalafmt::test
