#!/bin/bash
# Abort on error, unitialized variables and pipe errors
set -eu
#set -v

export PING_SLEEP=30s
export WORKDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export BUILD_OUTPUT=$WORKDIR/build.out
export TEST_PROC_LOG_OUTPUT=$WORKDIR/test-proc.out
export TEST_API_LOG_OUTPUT=$WORKDIR/test-api.out

DUMP_LINES_TEST_PROC=20000
DUMP_LINES_TEST_API=2000
DUMP_LINES_BUILD=2000

touch $BUILD_OUTPUT
touch $TEST_PROC_LOG_OUTPUT
touch $TEST_API_LOG_OUTPUT


print_log_separator() {
  echo "----------------------------------------------------------------"
  echo "-"
  echo "-"
  echo "-"
  echo "-"
  echo "-"
  echo "-"
  echo "----------------------------------------------------------------"
}

dump_output() {
  if [ "$DUMP_LINES_BUILD" -eq "-1" ]; then
    echo "Printing all the $1 output:"
    cat $1 
  else
    echo "Tailing the last $DUMP_LINES_BUILD lines of build output:"
    tail -$2 $1
  fi
}

print_log() {
  print_log_separator
  dump_output $BUILD_OUTPUT $DUMP_LINES_BUILD

  print_log_separator
  dump_output $TEST_PROC_LOG_OUTPUT $DUMP_LINES_TEST_PROC

  print_log_separator
  dump_output $TEST_API_LOG_OUTPUT $DUMP_LINES_TEST_API
}

run_build() {
  #./gradlew build --full-stacktrace --debug 2>&1 | tee >(grep TestEventLogger | grep -P -n "[[:ascii:]]" >> $TEST_LOG_OUTPUT) | grep  -P -n "[[:ascii:]]" >> $BUILD_OUTPUT
  ./gradlew assemble --full-stacktrace --debug >> $BUILD_OUTPUT
}

run_tests() {
  # enable debug output here to spot the errors
  ./gradlew test --full-stacktrace --debug --tests=tika.LegacyTikaProcessorTests >> $TEST_PROC_LOG_OUTPUT
  ./gradlew test --full-stacktrace --debug --tests=tika.CompositeTikaProcessorTests >> $TEST_PROC_LOG_OUTPUT
  # disable debug here, too much verbose
  ./gradlew test --full-stacktrace --tests=ServiceControllerTests >> $TEST_API_LOG_OUTPUT
  ./gradlew test --full-stacktrace --tests=ServiceControllerDocumentMultipartFileTests >> $TEST_API_LOG_OUTPUT
  ./gradlew test --full-stacktrace --tests=ServiceControllerDocumentStreamTests >> $TEST_API_LOG_OUTPUT
}


error_handler() {
  echo ERROR: An error was encountered with the build.
  print_log
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

run_tests


# print the log
print_log


# nicely terminate the ping output loop
kill $PING_LOOP_PID
