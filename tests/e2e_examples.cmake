# Runs the three example jars end-to-end and compares stdout exactly.

foreach(var JVM CLASSES EX_DIR SAMPLE TODO_FILE LIB_DIR)
    if(NOT DEFINED ${var})
        message(FATAL_ERROR "e2e_examples.cmake: ${var} is not set")
    endif()
endforeach()

function(run_expect name expected)
    execute_process(
        COMMAND ${ARGN}
        RESULT_VARIABLE rc
        OUTPUT_VARIABLE out
        ERROR_VARIABLE err)
    if(NOT rc EQUAL 0)
        message(FATAL_ERROR "${name}: exit code ${rc}\nstdout:\n${out}\nstderr:\n${err}")
    endif()
    if(NOT out STREQUAL expected)
        message(FATAL_ERROR "${name}: stdout mismatch\nexpected:\n---\n${expected}\n---\nactual:\n---\n${out}\n---\nstderr:\n${err}")
    endif()
    message(STATUS "${name}: ok")
endfunction()

run_expect("wordcount"
"lines=3
words=13
chars=60
top=the:3,fox:2,jumps:2
" ${JVM} -cp ${CLASSES} -jar ${EX_DIR}/WordCount.jar ${SAMPLE})

run_expect("wordcount -Dwordcount.top=1"
"lines=3
words=13
chars=60
top=the:3
" ${JVM} -Dwordcount.top=1 -cp ${CLASSES} -jar ${EX_DIR}/WordCount.jar ${SAMPLE})

run_expect("configreader"
"app.name=darks-demo
app.version=1.0
owner=CI
mode=prod
keys=4
bigOk=true
empty=0
" ${JVM} -Dconfig.owner=CI -cp ${CLASSES} -jar ${EX_DIR}/ConfigReader.jar)

file(REMOVE "${TODO_FILE}")

run_expect("todolist add 1"
"added=write report
count=1
native=2
" ${JVM} -Dtodo.file=${TODO_FILE} -Djava.library.path=${LIB_DIR}
  -cp ${CLASSES} -jar ${EX_DIR}/TodoList.jar add "write report")

run_expect("todolist add 2"
"added=ship build
count=2
native=3
" ${JVM} -Dtodo.file=${TODO_FILE} -Djava.library.path=${LIB_DIR}
  -cp ${CLASSES} -jar ${EX_DIR}/TodoList.jar add "ship build")

run_expect("todolist list"
"task0=write report
task1=ship build
count=2
native=3
" ${JVM} -Dtodo.file=${TODO_FILE} -Djava.library.path=${LIB_DIR}
  -cp ${CLASSES} -jar ${EX_DIR}/TodoList.jar list)

message(STATUS "all example jars: ok")
