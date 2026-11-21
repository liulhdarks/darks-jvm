# Replay: record one run's decision log, then replay it and require the same
# Java-visible trace and a matched event hash.
#
# Required: JVM, CLASSES, WORK_DIR.

if(NOT DEFINED JVM OR NOT DEFINED CLASSES OR NOT DEFINED WORK_DIR)
    message(FATAL_ERROR "deterministic_replay.cmake: JVM, CLASSES and WORK_DIR are required")
endif()

set(log_file "${WORK_DIR}/deterministic-replay.log")
file(REMOVE "${log_file}")

execute_process(
    COMMAND "${JVM}" -Ddarks.deterministic=99 "-Ddarks.record=${log_file}"
            -cp "${CLASSES}" DeterministicReplay
    RESULT_VARIABLE rc
    OUTPUT_VARIABLE rec_out
    ERROR_VARIABLE rec_err
    TIMEOUT 120)
if(NOT rc EQUAL 0)
    message(FATAL_ERROR "deterministic_replay: record run exited ${rc}\n${rec_out}${rec_err}")
endif()
if(NOT EXISTS "${log_file}")
    message(FATAL_ERROR "deterministic_replay: record file was not written")
endif()
set(rec_all "${rec_out}${rec_err}")
string(REGEX MATCH "deterministic-replay rnd=[^\r\n]*" rec_rnd "${rec_all}")
string(REGEX MATCH "deterministic-replay keys=[^\r\n]*" rec_keys "${rec_all}")
string(REGEX MATCH "deterministic: seed=[^\r\n]*" rec_hash "${rec_all}")
if("${rec_rnd}" STREQUAL "" OR "${rec_keys}" STREQUAL "" OR "${rec_hash}" STREQUAL "")
    message(FATAL_ERROR "deterministic_replay: record run missing output\n${rec_all}")
endif()

execute_process(
    COMMAND "${JVM}" "-Ddarks.replay=${log_file}" -cp "${CLASSES}" DeterministicReplay
    RESULT_VARIABLE rc2
    OUTPUT_VARIABLE rep_out
    ERROR_VARIABLE rep_err
    TIMEOUT 120)
if(NOT rc2 EQUAL 0)
    message(FATAL_ERROR "deterministic_replay: replay run exited ${rc2}\n${rep_out}${rep_err}")
endif()
set(rep_all "${rep_out}${rep_err}")
string(REGEX MATCH "deterministic-replay rnd=[^\r\n]*" rep_rnd "${rep_all}")
string(REGEX MATCH "deterministic-replay keys=[^\r\n]*" rep_keys "${rep_all}")
string(REGEX MATCH "deterministic-replay: seed=[^\r\n]* matched=true" rep_hash "${rep_all}")
if("${rep_hash}" STREQUAL "")
    message(FATAL_ERROR "deterministic_replay: replay did not match\n${rep_all}")
endif()
if(NOT "${rep_rnd}" STREQUAL "${rec_rnd}" OR NOT "${rep_keys}" STREQUAL "${rec_keys}")
    message(FATAL_ERROR
        "deterministic_replay: Java trace differs\n  record: ${rec_rnd} / ${rec_keys}\n  replay: ${rep_rnd} / ${rep_keys}")
endif()
message(STATUS "deterministic_replay: ${rec_hash}\n  replay: ${rep_hash}")
