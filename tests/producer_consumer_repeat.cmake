# Runs the producer/consumer demo repeatedly, alternating the single-threaded
# and multi-threaded scheduler modes.  A broken wakeup or join typically shows
# up as a timeout or a validation failure within these runs.

foreach(var JVM CLASSES REPEAT)
    if(NOT DEFINED ${var})
        message(FATAL_ERROR "producer_consumer_repeat.cmake: ${var} is not set")
    endif()
endforeach()

math(EXPR LAST "${REPEAT} - 1")
foreach(i RANGE 0 ${LAST})
    math(EXPR rem "${i} % 2")
    if(rem EQUAL 0)
        set(mp 1)
    else()
        set(mp 4)
    endif()
    execute_process(
        COMMAND "${CMAKE_COMMAND}" -E env "DARKS_JVM_MP=${mp}"
                "${JVM}" -cp "${CLASSES}" ProducerConsumerDemo
        RESULT_VARIABLE rc
        OUTPUT_VARIABLE out
        ERROR_VARIABLE err
        TIMEOUT 60)
    if(NOT rc EQUAL 0)
        message(FATAL_ERROR
            "producer_consumer_repeat: run ${i} (MP=${mp}) failed with ${rc}\n"
            "stdout:\n${out}\nstderr:\n${err}")
    endif()
    if(NOT out MATCHES "producer-consumer ok")
        message(FATAL_ERROR
            "producer_consumer_repeat: run ${i} (MP=${mp}) did not validate\n"
            "stdout:\n${out}\nstderr:\n${err}")
    endif()
    message(STATUS "producer-consumer run ${i} (MP=${mp}): ok")
endforeach()
message(STATUS "producer-consumer repeated ${REPEAT} times: ok")
