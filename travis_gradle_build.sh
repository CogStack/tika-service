#!/bin/bash
# Abort on Error
set -e
#set -v

export PING_SLEEP=30s
export WORKDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export BUILD_OUTPUT=$WORKDIR/build.out
export TEST_LOG_OUTPUT=$WORKDIR/testlog.out

DUMP_LINES_BUILD=2000
DUMP_LINES_TEST=2000

touch $BUILD_OUTPUT
touch $TEST_LOG_OUTPUT

print_log_separator() {
  echo "----------------------------------------------------------------"
  echo ""
  echo ""
  echo ""
  echo ""
  echo "----------------------------------------------------------------"
}

dump_build_output() {
  if [ "$DUMP_LINES_BUILD" -eq "-1" ]; then
    echo "Printing all the build output:"
    cat $BUILD_OUTPUT 
  else
    echo "Tailing the last $DUMP_LINES_BUILD lines of build output:"
    tail -$DUMP_LINES_BUILD $BUILD_OUTPUT
  fi
}

dump_test_output() {
  if [ "$DUMP_LINES_TEST" -eq "-1" ]; then
    echo "Printing all the tests logger output:"
    cat $TEST_LOG_OUTPUT
  else 
    echo "Tailing the last $DUMP_LINES_TEST lines of build output:"
    tail -$DUMP_LINES_TEST $TEST_LOG_OUTPUT
  fi
}

run_build() {
  ./gradlew build --full-stacktrace --debug 2>&1 | tee >(grep TestEventLogger | grep -P -n "[[:ascii:]]" >> $TEST_LOG_OUTPUT) | grep  -P -n "[[:ascii:]]" >> $BUILD_OUTPUT
}

error_handler() {
  echo ERROR: An error was encountered with the build.
  dump_output
  exit 1
}

# If an error occurs, run our error handler to output a tail of the build
trap 'error_handler' ERR

# Set up a repeating loop to send some output to Travis.
bash -c "while true; do echo \$(date) - building ...; sleep $PING_SLEEP; done" &
PING_LOOP_PID=$!

# Build Commands
#./gradlew build --stacktrace --debug  >> $BUILD_OUTPUT 2>&1
run_build

# The build finished without returning an error so dump a tail of the output
print_log_separator
dump_build_output

print_log_separator
dump_test_output

# nicely terminate the ping output loop
kill $PING_LOOP_PID
