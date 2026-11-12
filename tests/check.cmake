# Test driver: runs a command and checks its exit code and output.
#
# Required: PROG, EXPECT_RC.
# Optional: ARGS (list), EXPECT_TEXT (CMake regex).

if(NOT DEFINED PROG)
    message(FATAL_ERROR "check.cmake: PROG is not set")
endif()

execute_process(
    COMMAND "${PROG}" ${ARGS}
    RESULT_VARIABLE rc
    OUTPUT_VARIABLE out
    ERROR_VARIABLE err
)

set(all_output "${out}${err}")

if(NOT "${rc}" STREQUAL "${EXPECT_RC}")
    message(FATAL_ERROR
        "check.cmake: expected exit code ${EXPECT_RC}, got ${rc}\n"
        "command: ${PROG} ${ARGS}\n"
        "output:\n${all_output}")
endif()

if(DEFINED EXPECT_TEXT)
    if(NOT all_output MATCHES "${EXPECT_TEXT}")
        message(FATAL_ERROR
            "check.cmake: output did not match '${EXPECT_TEXT}'\n"
            "command: ${PROG} ${ARGS}\n"
            "output:\n${all_output}")
    endif()
endif()

if(DEFINED EXPECT_TEXT2)
    if(NOT all_output MATCHES "${EXPECT_TEXT2}")
        message(FATAL_ERROR
            "check.cmake: output did not match '${EXPECT_TEXT2}'\n"
            "command: ${PROG} ${ARGS}\n"
            "output:\n${all_output}")
    endif()
endif()

if(DEFINED EXPECT_TEXT3)
    if(NOT all_output MATCHES "${EXPECT_TEXT3}")
        message(FATAL_ERROR
            "check.cmake: output did not match '${EXPECT_TEXT3}'\n"
            "command: ${PROG} ${ARGS}\n"
            "output:\n${all_output}")
    endif()
endif()
