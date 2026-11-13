# CTest definitions: one unit-test binary plus end-to-end CLI checks.

find_program(JAVAC NAMES javac)
find_program(JAVA NAMES java)

add_executable(unit_tests unit_tests.cpp)
target_link_libraries(unit_tests PRIVATE darksvm)

add_executable(gc_tests gc_tests.cpp)
target_link_libraries(gc_tests PRIVATE darksvm)

if(ZLIB_FOUND)
    add_executable(zip_tests zip_tests.cpp)
    target_link_libraries(zip_tests PRIVATE darksvm)
endif()

set(CLASSES_DIR "${CMAKE_BINARY_DIR}/classes")

if(JAVAC)
    file(GLOB RUNTIME_JAVA_SOURCES
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/io/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/function/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/reflect/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/annotation/*.java"
    )
    set(JAVA_SOURCES
        ${RUNTIME_JAVA_SOURCES}
        "${CMAKE_CURRENT_SOURCE_DIR}/java/Hello.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/Clock.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/GcStress.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/dev/darks/jni/NativeLib.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/ThreadBasics.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/CoroutineScale.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/ProducerConsumer.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/VolatileVisibility.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/SyncContention.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/SleepConcurrency.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/InterruptSleep.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/JoinTimeoutTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/GcThreads.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/ReflectionAdvanced.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/JitWorkload.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/CollectionsAdvanced.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/RuntimeCompat.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/LongArrayInit.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SystemProps.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/FileIoDemo.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/ResourceDemo.java"
    )
    set(CLASS_STAMP "${CLASSES_DIR}/.stamp")
    add_custom_command(
        OUTPUT "${CLASS_STAMP}"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${CLASSES_DIR}"
        COMMAND "${JAVAC}" -source 8 -target 8 -d "${CLASSES_DIR}" ${JAVA_SOURCES}
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${CLASSES_DIR}/compat"
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different
                "${CMAKE_CURRENT_SOURCE_DIR}/resources/compat/message.txt"
                "${CLASSES_DIR}/compat/message.txt"
        COMMAND "${CMAKE_COMMAND}" -E touch "${CLASS_STAMP}"
        DEPENDS ${JAVA_SOURCES}
        COMMENT "Compiling the minimal Java runtime and test classes"
        VERBATIM
    )
    add_custom_target(java_classes ALL DEPENDS "${CLASS_STAMP}")
    add_dependencies(unit_tests java_classes)
    add_dependencies(gc_tests java_classes)

    set(CHECK "${CMAKE_CURRENT_SOURCE_DIR}/check.cmake")
    set(JVM "$<TARGET_FILE:darks-jvm>")

    # ------------------------------------------------------------------
    # Java 11 bytecode (major version 55) and language-feature coverage.
    # These sources are compiled with --release 11 so the produced class
    # files exercise the newer constant-pool tags and invokedynamic paths.
    # ------------------------------------------------------------------
    execute_process(COMMAND "${JAVAC}" --version
                    OUTPUT_VARIABLE JAVAC_VERSION
                    ERROR_VARIABLE JAVAC_VERSION_ERROR)
    if(NOT JAVAC_VERSION)
        execute_process(COMMAND "${JAVAC}" -version
                        OUTPUT_QUIET ERROR_VARIABLE JAVAC_VERSION)
    endif()
    string(STRIP "${JAVAC_VERSION}${JAVAC_VERSION_ERROR}" JAVAC_VERSION)
    string(REGEX MATCH "[0-9]+" JAVAC_MAJOR "${JAVAC_VERSION}")
    if(JAVAC_MAJOR GREATER_EQUAL 11)
        file(GLOB JAVA11_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/java11/*.java")
        set(JAVA11_STAMP "${CLASSES_DIR}/.stamp_java11")
        add_custom_command(
            OUTPUT "${JAVA11_STAMP}"
            COMMAND "${JAVAC}" --release 11 -d "${CLASSES_DIR}" ${JAVA11_SOURCES}
            COMMAND "${CMAKE_COMMAND}" -E touch "${JAVA11_STAMP}"
            DEPENDS ${JAVA11_SOURCES}
            COMMENT "Compiling Java 11 language-feature tests")
        add_custom_target(java11_classes ALL DEPENDS "${JAVA11_STAMP}")
        add_dependencies(java11_classes java_classes)

        add_test(NAME java11_language
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};LangFeatures" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=summary=dflt:named:n!:static,conflict:alpha/beta,lam:5:8:6:4:42:0,anonlocal:6,nested:9:6,var:10,auto:8:4,enum:3:1:GREEN:g:ABother,twr:5,multi:caught:ise:caught:nfe,sup:primary:1:close-x,sync:4:5:5,init:5,vol:12,bridge:n5"
                    "-DEXPECT_TEXT2=java11\ language:\ ok" -P "${CHECK}")

        add_test(NAME java11_collections
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};CollectionsDemo" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=coll=2:2:true:5:3:20:2:3:6:9:5:8:12:7:0:3:1:0"
                    "-DEXPECT_TEXT2=java11\ collections:\ ok" -P "${CHECK}")

        add_test(NAME java11_reflect
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};ReflectDemo" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=refl=java.lang.Object:7:3:4:42:40:1:1:1:1:1"
                    "-DEXPECT_TEXT2=java11\ reflect:\ ok" -P "${CHECK}")

        add_test(NAME reflect_advanced
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};ReflectionAdvanced" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=reflect-adv\ checks=.*\ failures=0"
                    "-DEXPECT_TEXT2=reflection\ advanced:\ ok" -P "${CHECK}")

        # The same workload runs with the lightweight JIT disabled, enabled
        # with a one-invocation threshold, and with a high threshold (pure
        # interpreter path). All three must produce identical output.
        add_test(NAME collections_advanced
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};CollectionsAdvanced" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=collections\ checks=.*\ failures=0"
                    "-DEXPECT_TEXT2=collections\ advanced:\ ok" -P "${CHECK}")

        add_test(NAME jit_off
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};JitWorkload" -DEXPECT_RC=0
                    "-DENV_VARS=DARKS_JVM_JIT=0"
                    "-DEXPECT_TEXT=jit\ mix=.*\ failures=0"
                    "-DEXPECT_TEXT2=jit\ workload:\ ok" -P "${CHECK}")

        add_test(NAME jit_on
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};JitWorkload" -DEXPECT_RC=0
                    "-DENV_VARS=DARKS_JVM_JIT=1;DARKS_JVM_JIT_THRESHOLD=1;DARKS_JVM_JIT_VERBOSE=1"
                    "-DEXPECT_TEXT=jit\ mix=.*\ failures=0"
                    "-DEXPECT_TEXT2=jit\ workload:\ ok"
                    "-DEXPECT_TEXT3=compiled JitWorkload" -P "${CHECK}")

        add_test(NAME jit_threshold
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};JitWorkload" -DEXPECT_RC=0
                    "-DENV_VARS=DARKS_JVM_JIT=1;DARKS_JVM_JIT_THRESHOLD=1000"
                    "-DEXPECT_TEXT=jit\ mix=.*\ failures=0"
                    "-DEXPECT_TEXT2=jit\ workload:\ ok" -P "${CHECK}")

        # module-info.class (CONSTANT_Module / CONSTANT_Package) must parse,
        # and a class inside the module must still run from the classpath.
        file(GLOB JAVA11_MODULE_SOURCES
            "${CMAKE_CURRENT_SOURCE_DIR}/java11mod/module-info.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java11mod/pkg/Main.java")
        set(JAVA11_MODULE_STAMP "${CLASSES_DIR}/.stamp_java11mod")
        add_custom_command(
            OUTPUT "${JAVA11_MODULE_STAMP}"
            COMMAND "${JAVAC}" --release 11 -d "${CLASSES_DIR}" ${JAVA11_MODULE_SOURCES}
            COMMAND "${CMAKE_COMMAND}" -E touch "${JAVA11_MODULE_STAMP}"
            DEPENDS ${JAVA11_MODULE_SOURCES}
            COMMENT "Compiling the Java 11 module-info fixture")
        add_custom_target(java11_module_classes ALL DEPENDS "${JAVA11_MODULE_STAMP}")
        add_dependencies(java11_module_classes java_classes)

        add_test(NAME java11_module_dump
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};--dump;module-info" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=this_class:\ \ module-info" -P "${CHECK}")

        add_test(NAME java11_module_run
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};pkg.Main" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=module\ main" -P "${CHECK}")

        # Newer class files must be rejected with a clear diagnostic, not
        # mis-parsed.  Only possible when the installed javac is newer than 11.
        if(JAVAC_MAJOR GREATER_EQUAL 12)
            set(JAVA11_REJECT_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/java11reject/High.java")
            set(JAVA11_REJECT_STAMP "${CLASSES_DIR}/.stamp_java11reject")
            add_custom_command(
                OUTPUT "${JAVA11_REJECT_STAMP}"
                COMMAND "${JAVAC}" --release ${JAVAC_MAJOR} -d "${CLASSES_DIR}"
                        ${JAVA11_REJECT_SOURCES}
                COMMAND "${CMAKE_COMMAND}" -E touch "${JAVA11_REJECT_STAMP}"
                DEPENDS ${JAVA11_REJECT_SOURCES}
                COMMENT "Compiling a post-Java-11 class for the rejection test")
            add_custom_target(java11_reject_classes ALL DEPENDS "${JAVA11_REJECT_STAMP}")
            add_dependencies(java11_reject_classes java_classes)

            add_test(NAME java11_reject_newer
                COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                        "-DARGS=-cp;${CLASSES_DIR};High" -DEXPECT_RC=3
                        "-DEXPECT_TEXT=unsupported\ class\ file\ version" -P "${CHECK}")
        endif()
    else()
        message(STATUS "javac ${JAVAC_VERSION} is older than 11: Java 11 tests disabled.")
    endif()

    # ------------------------------------------------------------------
    # Third-party native library compiled with the *official* JDK jni.h.
    # ------------------------------------------------------------------
    get_filename_component(JAVAC_REAL "${JAVAC}" REALPATH)
    get_filename_component(JDK_BIN_DIR "${JAVAC_REAL}" DIRECTORY)
    get_filename_component(JDK_HOME "${JDK_BIN_DIR}" DIRECTORY)

    # The JDK keeps jni.h in include/ but jni_md.h in an OS specific
    # subdirectory: include/linux, include/darwin or include/win32.  Older
    # JDKs put jni_md.h directly in include/, so keep that as a fallback.
    if(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
        set(JDK_JNI_PLATFORM_INCLUDE "${JDK_HOME}/include/darwin")
    elseif(CMAKE_SYSTEM_NAME STREQUAL "Windows")
        set(JDK_JNI_PLATFORM_INCLUDE "${JDK_HOME}/include/win32")
    elseif(CMAKE_SYSTEM_NAME STREQUAL "Linux")
        set(JDK_JNI_PLATFORM_INCLUDE "${JDK_HOME}/include/linux")
    else()
        # Other Unixes (FreeBSD, AIX, ...) use a lower case directory name.
        string(TOLOWER "${CMAKE_SYSTEM_NAME}" JDK_JNI_PLATFORM_NAME)
        set(JDK_JNI_PLATFORM_INCLUDE "${JDK_HOME}/include/${JDK_JNI_PLATFORM_NAME}")
    endif()
    if(NOT EXISTS "${JDK_JNI_PLATFORM_INCLUDE}/jni_md.h" AND
       EXISTS "${JDK_HOME}/include/jni_md.h")
        set(JDK_JNI_PLATFORM_INCLUDE "${JDK_HOME}/include")
    endif()

    if(EXISTS "${JDK_HOME}/include/jni.h" AND
       NOT EXISTS "${JDK_JNI_PLATFORM_INCLUDE}/jni_md.h")
        message(FATAL_ERROR
            "Found ${JDK_HOME}/include/jni.h but no jni_md.h in "
            "${JDK_JNI_PLATFORM_INCLUDE}. Set CMAKE_SYSTEM_NAME correctly or "
            "install a JDK whose include/ layout matches ${CMAKE_SYSTEM_NAME}.")
    endif()

    if(EXISTS "${JDK_HOME}/include/jni.h")
        add_library(thirdparty_native SHARED native/thirdparty.cpp)
        target_include_directories(thirdparty_native PRIVATE
            "${JDK_HOME}/include" "${JDK_JNI_PLATFORM_INCLUDE}")
        if(NOT WIN32)
            set_target_properties(thirdparty_native PROPERTIES PREFIX "lib")
        endif()

        add_test(NAME thirdparty_jni
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};dev.darks.jni.NativeLib;$<TARGET_FILE:thirdparty_native>"
                    -DEXPECT_RC=0
                    "-DEXPECT_TEXT=thirdparty jni checks: 14, failed: 0" -P "${CHECK}")

        add_test(NAME thirdparty_jni_unload
            COMMAND "${CMAKE_COMMAND}" -DPROG=${CMAKE_COMMAND}
                    "-DARGS=-E;env;DARKS_JNI_UNLOAD_MARKER=${CMAKE_BINARY_DIR}/jni_unload.marker;${JVM};-cp;${CLASSES_DIR};dev.darks.jni.NativeLib;$<TARGET_FILE:thirdparty_native>"
                    -DEXPECT_RC=0
                    "-DREMOVE_FILE=${CMAKE_BINARY_DIR}/jni_unload.marker"
                    "-DEXPECT_FILE=${CMAKE_BINARY_DIR}/jni_unload.marker"
                    -P "${CHECK}")
    endif()

    # ------------------------------------------------------------------
    # Executable jar support: an application jar that bundles the minimal
    # runtime is launched with -jar (Main-Class from the manifest) and is
    # also usable as a plain class path entry.
    # ------------------------------------------------------------------
    find_program(JAR_TOOL NAMES jar)
    if(JAR_TOOL)
        set(JAR_STAGE "${CMAKE_BINARY_DIR}/jarstage")
        set(TEST_APP_JAR "${CMAKE_BINARY_DIR}/app.jar")
        set(JAR_DEPS java_classes)
        if(DEFINED JAVA11_MODULE_STAMP)
            list(APPEND JAR_DEPS "${JAVA11_MODULE_STAMP}")
        endif()
        if(DEFINED JAVA11_REJECT_STAMP)
            list(APPEND JAR_DEPS "${JAVA11_REJECT_STAMP}")
        endif()
        add_custom_command(
            OUTPUT "${TEST_APP_JAR}"
            COMMAND "${CMAKE_COMMAND}" -E rm -rf "${JAR_STAGE}"
            COMMAND "${CMAKE_COMMAND}" -E copy_directory "${CLASSES_DIR}" "${JAR_STAGE}"
            COMMAND "${CMAKE_COMMAND}" -E rm -f "${JAR_STAGE}/module-info.class"
            COMMAND "${CMAKE_COMMAND}" -E rm -f "${JAR_STAGE}/High.class"
            COMMAND "${JAR_TOOL}" --create --file "${TEST_APP_JAR}"
                    --main-class Hello -C "${JAR_STAGE}" .
            DEPENDS ${JAR_DEPS}
            COMMENT "Packaging the executable jar fixture")
        add_custom_target(test_app_jar ALL DEPENDS "${TEST_APP_JAR}")
        add_dependencies(test_app_jar java_classes)
        if(TARGET java11_module_classes)
            add_dependencies(test_app_jar java11_module_classes)
        endif()
        if(TARGET java11_reject_classes)
            add_dependencies(test_app_jar java11_reject_classes)
        endif()

        add_test(NAME jar_main_class
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-jar;${TEST_APP_JAR}" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=Hello, darks-jvm!" -P "${CHECK}")
        add_test(NAME jar_classpath
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${TEST_APP_JAR};pkg.Main" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=module main" -P "${CHECK}")
    endif()

    add_test(NAME unit_tests
        COMMAND "${CMAKE_COMMAND}" -DPROG=$<TARGET_FILE:unit_tests>
                "-DARGS=${CLASSES_DIR}" -DEXPECT_RC=0
                -DEXPECT_TEXT=54/54 -P "${CHECK}")

    add_test(NAME gc_heap
        COMMAND "${CMAKE_COMMAND}" -DPROG=$<TARGET_FILE:gc_tests>
                "-DARGS=${CLASSES_DIR}" -DEXPECT_RC=0
                "-DEXPECT_TEXT=gc checks: [0-9]+/[0-9]+ passed" -P "${CHECK}")

    add_test(NAME gc_stress_survivors
        COMMAND "${CMAKE_COMMAND}" -DPROG=${CMAKE_COMMAND}
                "-DARGS=-E;env;DARKS_JVM_GC_THRESHOLD=4096;DARKS_JVM_GC_LOG=1;${JVM};-cp;${CLASSES_DIR};GcStress"
                -DEXPECT_RC=0 "-DEXPECT_TEXT=gc: local array survived"
                "-DEXPECT_TEXT2=gc: static array survived"
                "-DEXPECT_TEXT3=gc: local chain survived" -P "${CHECK}")

    add_test(NAME gc_stress_collect
        COMMAND "${CMAKE_COMMAND}" -DPROG=${CMAKE_COMMAND}
                "-DARGS=-E;env;DARKS_JVM_GC_THRESHOLD=4096;DARKS_JVM_GC_LOG=1;${JVM};-cp;${CLASSES_DIR};GcStress"
                -DEXPECT_RC=0 "-DEXPECT_TEXT=gc-collect" -P "${CHECK}")

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

    if(TARGET zip_tests)
        file(MAKE_DIRECTORY "${CMAKE_BINARY_DIR}/ziptests")
        add_test(NAME zip_edge_cases
            COMMAND "${CMAKE_COMMAND}" -DPROG=$<TARGET_FILE:zip_tests>
                    "-DARGS=${CMAKE_BINARY_DIR}/ziptests" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=zip\ checks:\ [0-9]+, failed:\ 0" -P "${CHECK}")
    endif()

    # A JDK-style java.lang.Object registers its natives from <clinit>.
    # Placing this class first on the class path proves that the bootstrap
    # hook resolves to Java_java_lang_Object_registerNatives.
    set(JDKOBJECT_DIR "${CMAKE_BINARY_DIR}/jdkobject")
    add_custom_command(
        OUTPUT "${JDKOBJECT_DIR}/java/lang/Object.class"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${JDKOBJECT_DIR}"
        COMMAND "${JAVAC}" --release 8 -d "${JDKOBJECT_DIR}"
                "${CMAKE_CURRENT_SOURCE_DIR}/java/jdkobject/Object.java"
        DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/java/jdkobject/Object.java"
        COMMENT "Compiling the JDK-style Object fixture")
    add_custom_target(jdkobject_classes ALL
        DEPENDS "${JDKOBJECT_DIR}/java/lang/Object.class")
    add_test(NAME object_jdk_clinit
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${JDKOBJECT_DIR}:${CLASSES_DIR};Hello"
                -DEXPECT_RC=0 "-DEXPECT_TEXT=Hello,\ darks-jvm!" -P "${CHECK}")

    add_test(NAME runtime_compat
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};RuntimeCompat" -DEXPECT_RC=0
                "-DEXPECT_TEXT=runtime-compat\ checks=[0-9]+\ failures=0" -P "${CHECK}")

    add_test(NAME long_array_init
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};LongArrayInit" -DEXPECT_RC=0
                "-DEXPECT_TEXT=long-array\ checks=3\ failures=0" -P "${CHECK}")

    add_test(NAME system_props
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-Dcompat.flag=on;-cp;${CLASSES_DIR};SystemProps" -DEXPECT_RC=0
                "-DEXPECT_TEXT=props-required=ok"
                "-DEXPECT_TEXT2=props-flag=on"
                "-DEXPECT_TEXT3=props-vendor=darks-jvm" -P "${CHECK}")

    add_test(NAME file_io
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};FileIoDemo" -DEXPECT_RC=0
                "-DEXPECT_TEXT=file-io\ checks=[0-9]+\ failures=0" -P "${CHECK}")

    add_test(NAME resource_io
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};ResourceDemo" -DEXPECT_RC=0
                "-DEXPECT_TEXT=resource\ checks=5\ failures=0" -P "${CHECK}")

    add_test(NAME version
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=--version" -DEXPECT_RC=0
                -DEXPECT_TEXT=darks-jvm\ 1.0 -P "${CHECK}")

    add_test(NAME usage_exit2
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                -DEXPECT_RC=2 -DEXPECT_TEXT=Usage: -P "${CHECK}")

    # ------------------------------------------------------------------
    # Coroutine / JMM acceptance tests.  Each runs once with the default
    # deterministic single OS thread (DARKS_JVM_MP=1) and once with four
    # OS threads (M:N scheduling, work stealing, MP-safe GC safepoints).
    # ------------------------------------------------------------------
    function(add_thread_test name expect)
        foreach(mp 1 4)
            add_test(NAME thread_${name}_mp${mp}
                COMMAND "${CMAKE_COMMAND}"
                    "-DPROG=${CMAKE_COMMAND}"
                    "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};${name}"
                    -DEXPECT_RC=0 -DTIMEOUT=180
                    "-DEXPECT_TEXT=${expect}" -P "${CHECK}")
        endforeach()
    endfunction()

    add_thread_test(ThreadBasics "thread-basics ok")
    add_thread_test(CoroutineScale "coroutines ok")
    add_thread_test(ProducerConsumer "producer-consumer ok")
    add_thread_test(VolatileVisibility "volatile ok")
    add_thread_test(SyncContention "sync ok")
    add_thread_test(SleepConcurrency "sleep-concurrency ok")
    add_thread_test(InterruptSleep "interrupt ok")
    add_thread_test(JoinTimeoutTest "join-timeout ok")

    # Continuous allocation with a low GC threshold: the collector must stop
    # every interpreter worker at a safepoint without deadlocking.
    foreach(mp 1 4)
        add_test(NAME thread_GcThreads_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};DARKS_JVM_GC_THRESHOLD=512;DARKS_JVM_GC_LOG=1;${JVM};-cp;${CLASSES_DIR};GcThreads"
                -DEXPECT_RC=0 -DTIMEOUT=180
                "-DEXPECT_TEXT=gc-threads ok" "-DEXPECT_TEXT2=gc-collect" -P "${CHECK}")
    endforeach()
    # ------------------------------------------------------------------
    # Example applications: compiled with --release 8, packaged into jars,
    # and executed with -jar (runtime classes come from build/classes).
    # ------------------------------------------------------------------
    if(JAR_TOOL)
        find_program(BASH_PROGRAM NAMES bash)
        set(EX_ROOT "${CMAKE_BINARY_DIR}/examples")
        set(EX_CLASSES "${EX_ROOT}/classes")
        set(EXAMPLES_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../examples")
        file(MAKE_DIRECTORY "${EX_ROOT}")
        # A >64KB resource exercises DEFLATE payloads larger than 64KB.
        set(BIG_CONTENT "")
        foreach(_i RANGE 1 3000)
            set(BIG_CONTENT "${BIG_CONTENT}0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\n")
        endforeach()
        file(WRITE "${EX_ROOT}/big.txt" "${BIG_CONTENT}")

        add_library(e2eadd SHARED native/e2e_add.cpp)

        add_custom_command(
            OUTPUT "${EX_ROOT}/WordCount.jar" "${EX_ROOT}/TodoList.jar"
                   "${EX_ROOT}/ConfigReader.jar"
            COMMAND "${CMAKE_COMMAND}" -E rm -rf "${EX_CLASSES}"
            COMMAND "${CMAKE_COMMAND}" -E make_directory "${EX_CLASSES}"
            COMMAND "${JAVAC}" --release 8 -d "${EX_CLASSES}"
                    "${EXAMPLES_DIR}/WordCount.java"
                    "${EXAMPLES_DIR}/TodoList.java"
                    "${EXAMPLES_DIR}/ConfigReader.java"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different
                    "${EXAMPLES_DIR}/resources/config.properties"
                    "${EX_CLASSES}/config.properties"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different
                    "${EXAMPLES_DIR}/resources/empty.txt"
                    "${EX_CLASSES}/empty.txt"
            COMMAND "${CMAKE_COMMAND}" -E copy_if_different
                    "${EX_ROOT}/big.txt" "${EX_CLASSES}/big.txt"
            COMMAND "${JAR_TOOL}" --create --file "${EX_ROOT}/WordCount.jar"
                    --main-class WordCount -C "${EX_CLASSES}" .
            COMMAND "${JAR_TOOL}" --create --file "${EX_ROOT}/TodoList.jar"
                    --main-class TodoList -C "${EX_CLASSES}" .
            COMMAND "${JAR_TOOL}" --create --file "${EX_ROOT}/ConfigReader.jar"
                    --main-class ConfigReader -C "${EX_CLASSES}" .
            DEPENDS
                "${EXAMPLES_DIR}/WordCount.java"
                "${EXAMPLES_DIR}/TodoList.java"
                "${EXAMPLES_DIR}/ConfigReader.java"
                "${EXAMPLES_DIR}/resources/config.properties"
                "${EXAMPLES_DIR}/resources/empty.txt"
            COMMENT "Compiling and packaging the example jars")
        add_custom_target(examples_jars ALL
            DEPENDS "${EX_ROOT}/WordCount.jar" "${EX_ROOT}/TodoList.jar"
                    "${EX_ROOT}/ConfigReader.jar")

        # ------------------------------------------------------------------
        # Manifest Class-Path: main.jar lists dep.jar and runs without -cp.
        # ------------------------------------------------------------------
        set(JARCOMPAT_ROOT "${CMAKE_BINARY_DIR}/jarcompat")
        set(JARCOMPAT_DEP "${JARCOMPAT_ROOT}/depclasses")
        set(JARCOMPAT_MAIN "${JARCOMPAT_ROOT}/mainclasses")
        # Kept outside JARCOMPAT_ROOT because the packaging step recreates that tree.
        set(JARCOMPAT_MANIFEST "${CMAKE_BINARY_DIR}/jarcompat-manifest.mf")
        file(WRITE "${JARCOMPAT_MANIFEST}"
            "Manifest-Version: 1.0\nMain-Class: Main2\nClass-Path: dep.jar\n")
        add_custom_command(
            OUTPUT "${JARCOMPAT_ROOT}/main.jar"
            COMMAND "${CMAKE_COMMAND}" -E rm -rf "${JARCOMPAT_ROOT}"
            COMMAND "${CMAKE_COMMAND}" -E make_directory "${JARCOMPAT_DEP}"
            COMMAND "${CMAKE_COMMAND}" -E make_directory "${JARCOMPAT_MAIN}"
            COMMAND "${JAVAC}" -source 8 -target 8 -d "${JARCOMPAT_DEP}"
                    "${CMAKE_CURRENT_SOURCE_DIR}/jarcompat/Dep.java"
            COMMAND "${JAVAC}" -source 8 -target 8 -cp "${JARCOMPAT_DEP}" -d "${JARCOMPAT_MAIN}"
                    "${CMAKE_CURRENT_SOURCE_DIR}/jarcompat/Main2.java"
            COMMAND "${JAR_TOOL}" --create --file "${JARCOMPAT_ROOT}/dep.jar"
                    -C "${JARCOMPAT_DEP}" .
            COMMAND "${JAR_TOOL}" --create --file "${JARCOMPAT_ROOT}/main.jar"
                    --manifest "${JARCOMPAT_MANIFEST}" -C "${JARCOMPAT_ROOT}/mainclasses" .
                    -C "${CLASSES_DIR}" .
            COMMAND "${CMAKE_COMMAND}" -E rm -f "${JARCOMPAT_MAIN}/module-info.class"
                    "${JARCOMPAT_MAIN}/High.class"
            DEPENDS
                "${CMAKE_CURRENT_SOURCE_DIR}/jarcompat/Dep.java"
                "${CMAKE_CURRENT_SOURCE_DIR}/jarcompat/Main2.java"
            COMMENT "Packaging the Manifest Class-Path fixture")
        add_custom_target(jar_classpath_fixture ALL
            DEPENDS "${JARCOMPAT_ROOT}/main.jar")
        add_test(NAME jar_manifest_classpath
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-jar;${JARCOMPAT_ROOT}/main.jar" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=dep-ok" -P "${CHECK}")

        add_test(NAME examples_e2e
            COMMAND "${CMAKE_COMMAND}"
                    -DJVM=${JVM}
                    -DCLASSES=${CLASSES_DIR}
                    -DEX_DIR=${EX_ROOT}
                    -DSAMPLE=${EXAMPLES_DIR}/data/sample.txt
                    -DTODO_FILE=${EX_ROOT}/todo.txt
                    -DLIB_DIR=$<TARGET_FILE_DIR:e2eadd>
                    -P "${CMAKE_CURRENT_SOURCE_DIR}/e2e_examples.cmake")
    endif()

else()
    message(WARNING "javac not found: end-to-end tests are disabled.")
    add_test(NAME unit_tests_without_classes
        COMMAND "${CMAKE_COMMAND}" -DPROG=$<TARGET_FILE:unit_tests>
                -DEXPECT_RC=0 -DEXPECT_TEXT=skipped -P "${CMAKE_CURRENT_SOURCE_DIR}/check.cmake")
endif()
