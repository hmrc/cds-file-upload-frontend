#!/usr/bin/env bash

sbt clean scalafmt test:scalafmt it:test::scalafmt test it:test scalafmt::test test:scalafmt::test
