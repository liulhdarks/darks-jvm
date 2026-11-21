# DeterministicRace: a fixed seed must reproduce the same interleaving, and
# different seeds must be able to produce different interleavings.
#
# Required: JVM, CLASSES.

if(NOT DEFINED JVM OR NOT DEFINED CLASSES)
    message(FATAL_ERROR "deterministic_race.cmake: JVM and CLASSES are required")
endif()

function(run_seed seed out_var)
    execute_process(
        COMMAND "${JVM}" "-Ddarks.deterministic=${seed}" -cp "${CLASSES}" DeterministicRace
        RESULT_VARIABLE rc
        OUTPUT_VARIABLE out
        ERROR_VARIABLE err
        TIMEOUT 120)
    if(NOT rc EQUAL 0)
        message(FATAL_ERROR "deterministic_race: seed ${seed} exited ${rc}\n${out}${err}")
    endif()
    set(all "${out}${err}")
    string(REGEX MATCH "deterministic-race interleaving=[^\r\n]*" line "${all}")
    if("${line}" STREQUAL "")
        message(FATAL_ERROR "deterministic_race: missing interleaving for seed ${seed}\n${all}")
    endif()
    set(${out_var} "${line}" PARENT_SCOPE)
endfunction()

run_seed(11 first)
run_seed(11 again)
if(NOT "${first}" STREQUAL "${again}")
    message(FATAL_ERROR "deterministic_race: seed 11 not reproducible\n  ${first}\n  ${again}")
endif()

set(variants 1)
foreach(seed 1 2 3 4 5 6 7 8)
    run_seed(${seed} line)
    if(NOT "${line}" STREQUAL "${first}")
        set(variants 2)
    endif()
endforeach()
if(variants LESS 2)
    message(FATAL_ERROR
        "deterministic_race: seeds 1..8 all produced '${first}'; the seed does not influence scheduling")
endif()
message(STATUS "deterministic_race: seed 11 reproducible (${first}), seeds vary")
