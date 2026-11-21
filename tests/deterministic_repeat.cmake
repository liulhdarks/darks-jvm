# DeterministicRepeat: run the same concurrent program REPEAT times under one
# seed and require an identical trace and decision-log hash every time.
#
# Required: JVM, CLASSES.  Optional: REPEAT (default 100).

if(NOT DEFINED JVM OR NOT DEFINED CLASSES)
    message(FATAL_ERROR "deterministic_repeat.cmake: JVM and CLASSES are required")
endif()
if(NOT DEFINED REPEAT)
    set(REPEAT 100)
endif()

set(expected "")
foreach(i RANGE 1 ${REPEAT})
    execute_process(
        COMMAND "${JVM}" -Ddarks.deterministic=12345 -cp "${CLASSES}" DeterministicRepeat
        RESULT_VARIABLE rc
        OUTPUT_VARIABLE out
        ERROR_VARIABLE err
        TIMEOUT 120)
    if(NOT rc EQUAL 0)
        message(FATAL_ERROR "deterministic_repeat: run ${i} exited ${rc}\n${out}${err}")
    endif()
    set(all "${out}${err}")
    if(NOT all MATCHES "deterministic-repeat enabled=true")
        message(FATAL_ERROR "deterministic_repeat: deterministic mode not active in run ${i}\n${all}")
    endif()
    string(REGEX MATCH "deterministic-repeat enabled=[^\r\n]*" trace "${all}")
    string(REGEX MATCH "deterministic: seed=[^\r\n]*" summary "${all}")
    if("${trace}" STREQUAL "" OR "${summary}" STREQUAL "")
        message(FATAL_ERROR "deterministic_repeat: missing trace/hash in run ${i}\n${all}")
    endif()
    set(observed "${trace}|${summary}")
    if(i EQUAL 1)
        set(expected "${observed}")
    elseif(NOT "${observed}" STREQUAL "${expected}")
        message(FATAL_ERROR
            "deterministic_repeat: run ${i} diverged\n  first: ${expected}\n  now:   ${observed}")
    endif()
endforeach()
message(STATUS "deterministic_repeat: ${REPEAT} identical runs: ${expected}")
