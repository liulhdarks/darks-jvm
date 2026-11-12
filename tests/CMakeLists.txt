# CTest definitions: one unit-test binary plus end-to-end CLI checks.

find_program(JAVAC NAMES javac)
find_program(JAVA NAMES java)

add_executable(unit_tests unit_tests.cpp)
target_link_libraries(unit_tests PRIVATE darksvm)

set(CLASSES_DIR "${CMAKE_BINARY_DIR}/classes")

if(JAVAC)
    set(JAVA_SOURCES
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/Object.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/String.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/System.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/io/PrintStream.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/Hello.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/Clock.java"
    )
    set(CLASS_STAMP "${CLASSES_DIR}/.stamp")
    add_custom_command(
        OUTPUT "${CLASS_STAMP}"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${CLASSES_DIR}"
        COMMAND "${JAVAC}" -source 8 -target 8 -d "${CLASSES_DIR}" ${JAVA_SOURCES}
        COMMAND "${CMAKE_COMMAND}" -E touch "${CLASS_STAMP}"
        DEPENDS ${JAVA_SOURCES}
        COMMENT "Compiling the minimal Java runtime and test classes"
        VERBATIM
    )
    add_custom_target(java_classes ALL DEPENDS "${CLASS_STAMP}")
    add_dependencies(unit_tests java_classes)

    set(CHECK "${CMAKE_CURRENT_SOURCE_DIR}/check.cmake")
    set(JVM "$<TARGET_FILE:darks-jvm>")

    add_test(NAME unit_tests
        COMMAND "${CMAKE_COMMAND}" -DPROG=$<TARGET_FILE:unit_tests>
                "-DARGS=${CLASSES_DIR}" -DEXPECT_RC=0
                -DEXPECT_TEXT=54/54 -P "${CHECK}")

    add_test(NAME dump_hello
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};--dump;Hello" -DEXPECT_RC=0
                -DEXPECT_TEXT=this_class:\ \ Hello -P "${CHECK}")

    add_test(NAME dump_clock
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};--dump;Clock" -DEXPECT_RC=0
                -DEXPECT_TEXT=methods: -P "${CHECK}")

    add_test(NAME hello
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};Hello" -DEXPECT_RC=0
                "-DEXPECT_TEXT=Hello, darks-jvm!" -P "${CHECK}")

    add_test(NAME clock_native
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};Clock" -DEXPECT_RC=0
                -DEXPECT_TEXT=entered\ native\ System -P "${CHECK}")

    add_test(NAME missing_class_exit4
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};NoSuchClass" -DEXPECT_RC=4
                -DEXPECT_TEXT=could\ not\ find -P "${CHECK}")

    add_test(NAME env_classpath
        COMMAND "${CMAKE_COMMAND}" -DPROG=${CMAKE_COMMAND}
                "-DARGS=-E;env;DARKS_JVM_CLASSPATH=${CLASSES_DIR};${JVM};Hello"
                -DEXPECT_RC=0 "-DEXPECT_TEXT=Hello, darks-jvm!" -P "${CHECK}")

    add_test(NAME version
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=--version" -DEXPECT_RC=0
                -DEXPECT_TEXT=darks-jvm\ 1.0 -P "${CHECK}")

    add_test(NAME usage_exit2
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                -DEXPECT_RC=2 -DEXPECT_TEXT=Usage: -P "${CHECK}")
else()
    message(WARNING "javac not found: end-to-end tests are disabled.")
    add_test(NAME unit_tests_without_classes
        COMMAND "${CMAKE_COMMAND}" -DPROG=$<TARGET_FILE:unit_tests>
                -DEXPECT_RC=0 -DEXPECT_TEXT=skipped -P "${CMAKE_CURRENT_SOURCE_DIR}/check.cmake")
endif()
