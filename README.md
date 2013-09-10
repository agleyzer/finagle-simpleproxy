## Finagle Proxy

Simple HTTP Proxy implemented using Finagle.

## Running

Use SBT:

    $ ./sbt
    > re-start

To stop in SBT (if SBT prompt is not visible because of console logs, press Return):

    > re-stop

## Configuration

Currently all configuration is done by passing command line parameters.
For more details, run the app with "-help" flag. By default, development
values are hard-coded (perhaps not the best decision).
