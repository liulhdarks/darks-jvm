# CTest definitions: one unit-test binary plus end-to-end CLI checks.

find_program(JAVAC NAMES javac)
find_program(JAVA NAMES java)

# The virtual-thread tests call Thread.ofVirtual()/Executors.newVirtualThread
# PerTaskExecutor(), which only exist in JDK 21+.  They are compiled with
# -source 11 -target 11 so they still produce class file version 55, but the
# compiler must be a JDK 21+ javac to see the API.  Prefer JAVA_HOME, which
# usually points at the newest installed JDK.
find_program(JAVAC21 NAMES javac HINTS "$ENV{JAVA_HOME}/bin" NO_DEFAULT_PATH)
if(NOT JAVAC21)
    find_program(JAVAC21 NAMES javac)
endif()
if(JAVAC21)
    execute_process(COMMAND "${JAVAC21}" -version
                    OUTPUT_VARIABLE JAVAC21_VERSION
                    ERROR_VARIABLE JAVAC21_VERSION_ERROR)
    string(STRIP "${JAVAC21_VERSION}${JAVAC21_VERSION_ERROR}" JAVAC21_VERSION)
    string(REGEX MATCH "[0-9]+" JAVAC21_MAJOR "${JAVAC21_VERSION}")
endif()

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
    file(GLOB RUNTIME_JAVA_SOURCES CONFIGURE_DEPENDS
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/io/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/net/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/function/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/concurrent/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/concurrent/atomic/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/concurrent/locks/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/reflect/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/invoke/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/ref/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/annotation/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/lang/management/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/org/xml/sax/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/beans/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/regex/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/stream/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/logging/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/math/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/text/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/time/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/time/format/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/time/temporal/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/time/chrono/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/nio/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/nio/channels/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/nio/channels/spi/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/nio/charset/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/nio/file/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/nio/file/attribute/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/security/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/org/ietf/jgss/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/javax/management/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/javax/management/modelmbean/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/javax/management/openmbean/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/security/cert/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/zip/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/java/util/jar/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/darks/*.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/../runtime/src/darks/concurrent/*.java"
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
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/MainMonitorHandoff.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/TimedWait.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/MonitorDeadlock.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/JoinCycle.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/ChannelCycle.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/TimedWaitNoFalsePositive.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/threads/Starvation.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/ReflectionAdvanced.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/GenericReflect.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/AnnotationDemo.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/ProxyDemo.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/BeansDemo.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/JitWorkload.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/CollectionsAdvanced.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/CollectionsCompat.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/BitSetTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/ProtectionDomainTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/RuntimeCompat.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/LongArrayInit.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/P0Checks.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/channel/ChannelPingPong.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/channel/ChannelClose.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/channel/SelectFairness.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/channel/SelectInterrupt.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/channel/ChannelGC.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/deterministic/DeterministicRepeat.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/deterministic/DeterministicRace.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/deterministic/VirtualClock.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/deterministic/DeterministicReplay.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/AssertCheck.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SystemProps.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/FileIoDemo.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/ResourceDemo.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkTimeTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkMathTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkTextTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkRegexTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkCharsetTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkClassLoaderTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkServiceLoaderTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkStackTraceTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkJarResourceTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/sdkcl/Plugin.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/sdkcl/PluginImpl.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/SdkUtilTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/M2InitLog.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/M2InitTarget.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/M2InitTarget2.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/M2Base.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/M2GoodChild.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/M2LoadingChecks.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/M2AnnotationChecks.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/M2DefaultMethodChecks.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/JdkIdentityHashMapTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/JdkWeakReferenceTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/JdkReflectArrayTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/JdkGcReferenceTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/JdkReflectionEqualityTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/JdkBitSetTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/M3CompatTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/M3ConcurrencyTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/M3NioTest.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/M3HttpLoopbackTest.java"
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
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${CLASSES_DIR}/sdkcl"
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different
                "${CMAKE_CURRENT_SOURCE_DIR}/resources/sdkcl/resource.txt"
                "${CLASSES_DIR}/sdkcl/resource.txt"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${CLASSES_DIR}/META-INF/services"
        COMMAND "${CMAKE_COMMAND}" -E copy_if_different
                "${CMAKE_CURRENT_SOURCE_DIR}/resources/services/META-INF/services/sdkcl.Plugin"
                "${CLASSES_DIR}/META-INF/services/sdkcl.Plugin"
        COMMAND "${CMAKE_COMMAND}" -E touch "${CLASS_STAMP}"
        DEPENDS ${JAVA_SOURCES}
        COMMENT "Compiling the minimal Java runtime and test classes"
        VERBATIM
    )
    add_custom_target(java_classes ALL DEPENDS "${CLASS_STAMP}")
    add_dependencies(unit_tests java_classes)
    add_dependencies(gc_tests java_classes)

    # Classes whose superclass/superinterface is deliberately absent at run
    # time.  They are compiled together with the missing base types, then the
    # base classes are removed from the output directory so that loading
    # exercises the missing-linkage path.
    set(M2_BROKEN_DIR "${CLASSES_DIR}/m2broken")
    set(M2_BROKEN_STAMP "${M2_BROKEN_DIR}/.stamp")
    set(M2_BROKEN_SOURCES
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/broken/M2MissingBase.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/broken/M2MissingIface.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/broken/M2BadSuper.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/broken/M2BadChild.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/broken/M2BadImpl.java"
        "${CMAKE_CURRENT_SOURCE_DIR}/java/m2loading/broken/M2BrokenChecks.java"
    )
    add_custom_command(
        OUTPUT "${M2_BROKEN_STAMP}"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${M2_BROKEN_DIR}"
        COMMAND "${JAVAC}" -source 8 -target 8 -cp "${CLASSES_DIR}"
                -d "${M2_BROKEN_DIR}" ${M2_BROKEN_SOURCES}
        COMMAND "${CMAKE_COMMAND}" -E rm -f
                "${M2_BROKEN_DIR}/M2MissingBase.class"
                "${M2_BROKEN_DIR}/M2MissingIface.class"
        COMMAND "${CMAKE_COMMAND}" -E touch "${M2_BROKEN_STAMP}"
        DEPENDS ${M2_BROKEN_SOURCES} "${CLASS_STAMP}"
        COMMENT "Compiling the missing-linkage fixtures"
        VERBATIM)
    add_custom_target(m2_broken_classes ALL DEPENDS "${M2_BROKEN_STAMP}")
    add_dependencies(m2_broken_classes java_classes)

    # Virtual-thread tests call Java 21-era APIs (Thread.ofVirtual,
    # Executors.newVirtualThreadPerTaskExecutor) that are not part of the
    # Java 11 API.  Compile them with -source 11 -target 11 (without
    # --release) so they bind to the runtime classes on the class path while
    # still producing class file version 55, which darks-jvm accepts.
    if(JAVAC21_MAJOR GREATER_EQUAL 21)
        set(CONCURRENT_TEST_SOURCES
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/VirtualThreadScale.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/ExecutorServiceTest.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/VirtualThreadInterruptTest.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/PinningTest.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/StructuredScopeTest.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/ConcurrentCollectionsTest.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/BlockingQueueTest.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/ExecutorPoolTest.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/ScheduledExecutorTest.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/LocksTest.java"
            "${CMAKE_CURRENT_SOURCE_DIR}/java/concurrent/AtomicsTest.java"
        )
        set(CONCURRENT_STAMP "${CLASSES_DIR}/.stamp_concurrent")
        add_custom_command(
            OUTPUT "${CONCURRENT_STAMP}"
            COMMAND "${CMAKE_COMMAND}" -E remove_directory "${CMAKE_CURRENT_BINARY_DIR}/jbase-patch"
            COMMAND "${CMAKE_COMMAND}" -E copy_directory
                    "${CLASSES_DIR}/java" "${CMAKE_CURRENT_BINARY_DIR}/jbase-patch/java"
            COMMAND "${JAVAC21}" -source 11 -target 11
                    --patch-module java.base=${CMAKE_CURRENT_BINARY_DIR}/jbase-patch
                    -cp "${CLASSES_DIR}"
                    -d "${CLASSES_DIR}" ${CONCURRENT_TEST_SOURCES}
            COMMAND "${CMAKE_COMMAND}" -E touch "${CONCURRENT_STAMP}"
            DEPENDS ${CONCURRENT_TEST_SOURCES} "${CLASS_STAMP}"
            COMMENT "Compiling virtual-thread tests")
        add_custom_target(concurrent_classes ALL DEPENDS "${CONCURRENT_STAMP}")
        add_dependencies(concurrent_classes java_classes)
    else()
        message(STATUS "javac 21 not found: virtual-thread tests disabled.")
    endif()

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

        add_test(NAME reflect_generics
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};GenericReflect" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=generic-reflect\ checks=.*\ failures=0"
                    "-DEXPECT_TEXT2=generic\ reflection:\ ok" -P "${CHECK}")

        add_test(NAME reflect_annotations
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};AnnotationDemo" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=annotation\ checks=.*\ failures=0"
                    "-DEXPECT_TEXT2=annotations:\ ok" -P "${CHECK}")

        # M2 class-loading and annotation semantics, run with both scheduler
        # modes so class metadata stays stable under 1 and 4 OS threads.
        foreach(mp 1 4)
            add_test(NAME m2_loading_semantics_mp${mp}
                COMMAND "${CMAKE_COMMAND}"
                    "-DPROG=${CMAKE_COMMAND}"
                    "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};M2LoadingChecks"
                    -DEXPECT_RC=0
                    "-DEXPECT_TEXT=m2\ loading\ checks=[0-9]+\ failures=0"
                    "-DEXPECT_TEXT2=m2\ loading:\ ok" -P "${CHECK}")

            add_test(NAME m2_annotation_semantics_mp${mp}
                COMMAND "${CMAKE_COMMAND}"
                    "-DPROG=${CMAKE_COMMAND}"
                    "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};M2AnnotationChecks"
                    -DEXPECT_RC=0
                    "-DEXPECT_TEXT=m2\ annotation\ checks=[0-9]+\ failures=0"
                    "-DEXPECT_TEXT2=m2\ annotations:\ ok" -P "${CHECK}")

            add_test(NAME m2_default_methods_mp${mp}
                COMMAND "${CMAKE_COMMAND}"
                    "-DPROG=${CMAKE_COMMAND}"
                    "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};M2DefaultMethodChecks"
                    -DEXPECT_RC=0
                    "-DEXPECT_TEXT=m2\ default-method\ checks=[0-9]+\ failures=0"
                    "-DEXPECT_TEXT2=m2\ default\ methods:\ ok" -P "${CHECK}")

            add_test(NAME m2_missing_linkage_mp${mp}
                COMMAND "${CMAKE_COMMAND}"
                    "-DPROG=${CMAKE_COMMAND}"
                    "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR}:${M2_BROKEN_DIR};M2BrokenChecks"
                    -DEXPECT_RC=0
                    "-DEXPECT_TEXT=m2\ broken-load\ checks=[0-9]+\ failures=0"
                    "-DEXPECT_TEXT2=m2\ broken\ loads:\ ok" -P "${CHECK}")
        endforeach()


        add_test(NAME reflect_proxy
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};ProxyDemo" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=proxy\ checks=.*\ failures=0"
                    "-DEXPECT_TEXT2=dynamic\ proxy:\ ok" -P "${CHECK}")

        add_test(NAME reflect_beans
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};BeansDemo" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=beans\ checks=.*\ failures=0"
                    "-DEXPECT_TEXT2=java\ beans:\ ok" -P "${CHECK}")

        # The same workload runs with the lightweight JIT disabled, enabled
        # with a one-invocation threshold, and with a high threshold (pure
        # interpreter path). All three must produce identical output.
        add_test(NAME collections_advanced
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};CollectionsAdvanced" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=collections\ checks=.*\ failures=0"
                    "-DEXPECT_TEXT2=collections\ advanced:\ ok" -P "${CHECK}")

        add_test(NAME collections_compat
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};CollectionsCompat" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=collections-compat:\ ok" -P "${CHECK}")

        add_test(NAME bitset
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};BitSetTest" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=bitset:\ ok" -P "${CHECK}")

        add_test(NAME protection_domain
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};ProtectionDomainTest" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=protectiondomain:\ ok" -P "${CHECK}")

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

        add_test(NAME sdk_jar_resource
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${TEST_APP_JAR};SdkJarResourceTest" -DEXPECT_RC=0
                    "-DEXPECT_TEXT=jar\ resource\ checks=[0-9]+\ failures=0"
                    "-DEXPECT_TEXT2=sdk\ jar\ resource:\ ok" -P "${CHECK}")
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

    add_test(NAME sdk_classloader
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};SdkClassLoaderTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=classloader\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=sdk\ classloader:\ ok" -P "${CHECK}")

    add_test(NAME sdk_service_loader
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};SdkServiceLoaderTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=service\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=sdk\ service\ loader:\ ok" -P "${CHECK}")

    add_test(NAME sdk_stack_trace
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};SdkStackTraceTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=stack\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=at\ SdkStackTraceTest.failure\\(SdkStackTraceTest.java:[0-9]+\\)"
                "-DEXPECT_TEXT3=sdk\ stack\ trace:\ ok" -P "${CHECK}")

    add_test(NAME sdk_time
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};SdkTimeTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=time\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=sdk\ time:\ ok" -P "${CHECK}")

    add_test(NAME sdk_math
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};SdkMathTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=math\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=sdk\ math:\ ok" -P "${CHECK}")

    add_test(NAME sdk_text
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};SdkTextTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=text\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=sdk\ text:\ ok" -P "${CHECK}")

    add_test(NAME sdk_regex
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};SdkRegexTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=regex\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=sdk\ regex:\ ok" -P "${CHECK}")

    add_test(NAME sdk_charset
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};SdkCharsetTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=charset\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=sdk\ charset:\ ok" -P "${CHECK}")

    add_test(NAME sdk_util
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};SdkUtilTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=util\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=sdk\ util:\ ok"
                "-DEXPECT_TEXT3=WARNING:\ log-warning-42" -P "${CHECK}")

    add_test(NAME jdk_identity_hashmap
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};JdkIdentityHashMapTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=identity\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=identity\ hashmap:\ ok" -P "${CHECK}")

    add_test(NAME jdk_weak_references
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};JdkWeakReferenceTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=reference\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=weak\ references:\ ok" -P "${CHECK}")

    add_test(NAME jdk_reflect_array
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};JdkReflectArrayTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=reflect-array\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=reflect\ array:\ ok" -P "${CHECK}")

    add_test(NAME jdk_gc_reference_stress
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};JdkGcReferenceTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=gc-reference\ rounds=100\ failures=0"
                "-DEXPECT_TEXT2=gc\ reference\ stress:\ ok" -P "${CHECK}")

    add_test(NAME jdk_reflection_equality
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};JdkReflectionEqualityTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=reflection-equality\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=reflection\ equality:\ ok" -P "${CHECK}")

    add_test(NAME jdk_bitset
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};JdkBitSetTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=bitset\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=bitset:\ ok" -P "${CHECK}")

    # M3 runtime surface: JMX, java.beans, charset, timer/executor polling,
    # NIO selector/socket channels and an end-to-end HTTP request/response.
    add_test(NAME m3_jdk_compat
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};M3CompatTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=m3\ compat\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=m3\ compat:\ ok" -P "${CHECK}")

    foreach(mp 1 4)
        add_test(NAME m3_concurrency_mp${mp}
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};M3ConcurrencyTest" -DEXPECT_RC=0
                    "-DENV_VARS=DARKS_JVM_MP=${mp}"
                    "-DEXPECT_TEXT=m3\ concurrency\ checks=[0-9]+\ failures=0"
                    "-DEXPECT_TEXT2=m3\ concurrency:\ ok" -P "${CHECK}")

        add_test(NAME m3_nio_mp${mp}
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${CLASSES_DIR};M3NioTest" -DEXPECT_RC=0
                    "-DENV_VARS=DARKS_JVM_MP=${mp}"
                    "-DEXPECT_TEXT=m3\ nio\ checks=[0-9]+\ failures=0"
                    "-DEXPECT_TEXT2=m3\ nio:\ ok" -P "${CHECK}")
    endforeach()

    add_test(NAME m3_http_loopback
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};M3HttpLoopbackTest" -DEXPECT_RC=0
                "-DEXPECT_TEXT=m3\ http\ loopback\ checks=[0-9]+\ failures=0"
                "-DEXPECT_TEXT2=m3\ http\ loopback:\ ok" -P "${CHECK}")

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

    # ------------------------------------------------------------------
    # P0/P1 semantics: array bounds/null/type checks, wide locals,
    # MIN_VALUE / -1 division, the Runtime subset, assertions, simple names,
    # thread states and hidden-field / inlined accessor behaviour.
    # ------------------------------------------------------------------
    add_test(NAME p0_p1_checks
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};P0Checks" -DEXPECT_RC=0
                "-DEXPECT_TEXT=p0\\ checks=[0-9]+\\ failures=0"
                "-DEXPECT_TEXT2=p0\\ checks:\\ ok" -P "${CHECK}")

    add_test(NAME assert_default_off
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${CLASSES_DIR};AssertCheck" -DEXPECT_RC=0
                "-DEXPECT_TEXT=assert-fired=false\\ msg=null"
                "-DEXPECT_TEXT2=assert-desired=false" -P "${CHECK}")

    add_test(NAME assert_enabled
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-ea;-cp;${CLASSES_DIR};AssertCheck" -DEXPECT_RC=0
                "-DEXPECT_TEXT=assert-fired=true\\ msg=assert-msg"
                "-DEXPECT_TEXT2=assert-desired=true" -P "${CHECK}")

    # ------------------------------------------------------------------
    # Link-time resolution failures.  Each fixture is compiled against a
    # stub API and then run with an incompatible class file, so the error
    # is raised when the call site is linked, as in a real deployment.
    # ------------------------------------------------------------------
    find_program(PYTHON3_EXECUTABLE NAMES python3)
    set(RES_DIR "${CMAKE_BINARY_DIR}/resolution")
    set(RES_SRC "${CMAKE_CURRENT_SOURCE_DIR}/resolution")

    add_custom_command(
        OUTPUT "${RES_DIR}/.method.stamp"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${RES_DIR}"
        COMMAND "${JAVAC}" -source 8 -target 8 -d "${RES_DIR}"
                "${RES_SRC}/CallNoMethod.java"
                "${RES_SRC}/stub/LibMissingMethod.java"
        COMMAND "${JAVAC}" -source 8 -target 8 -classpath "${RES_DIR}"
                -d "${RES_DIR}" "${RES_SRC}/runtime/LibMissingMethod.java"
        COMMAND "${CMAKE_COMMAND}" -E touch "${RES_DIR}/.method.stamp"
        DEPENDS "${RES_SRC}/CallNoMethod.java"
                "${RES_SRC}/stub/LibMissingMethod.java"
                "${RES_SRC}/runtime/LibMissingMethod.java"
        COMMENT "Compiling the NoSuchMethodError fixture"
        VERBATIM)

    add_custom_command(
        OUTPUT "${RES_DIR}/.field.stamp"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${RES_DIR}"
        COMMAND "${JAVAC}" -source 8 -target 8 -d "${RES_DIR}"
                "${RES_SRC}/CallNoField.java"
                "${RES_SRC}/stub/LibMissingField.java"
        COMMAND "${JAVAC}" -source 8 -target 8 -classpath "${RES_DIR}"
                -d "${RES_DIR}" "${RES_SRC}/runtime/LibMissingField.java"
        COMMAND "${CMAKE_COMMAND}" -E touch "${RES_DIR}/.field.stamp"
        DEPENDS "${RES_SRC}/CallNoField.java"
                "${RES_SRC}/stub/LibMissingField.java"
                "${RES_SRC}/runtime/LibMissingField.java"
        COMMENT "Compiling the NoSuchFieldError fixture"
        VERBATIM)

    add_custom_command(
        OUTPUT "${RES_DIR}/.class.stamp"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${RES_DIR}"
        COMMAND "${JAVAC}" -source 8 -target 8 -d "${RES_DIR}"
                "${RES_SRC}/CallNoClass.java" "${RES_SRC}/stub/Ghost.java"
        COMMAND "${CMAKE_COMMAND}" -E rm -f "${RES_DIR}/Ghost.class"
        COMMAND "${CMAKE_COMMAND}" -E touch "${RES_DIR}/.class.stamp"
        DEPENDS "${RES_SRC}/CallNoClass.java" "${RES_SRC}/stub/Ghost.java"
        COMMENT "Compiling the NoClassDefFoundError fixture"
        VERBATIM)

    add_custom_target(resolution_method_classes ALL
        DEPENDS "${RES_DIR}/.method.stamp")
    add_custom_target(resolution_field_classes ALL
        DEPENDS "${RES_DIR}/.field.stamp")
    add_custom_target(resolution_class_classes ALL
        DEPENDS "${RES_DIR}/.class.stamp")

    add_test(NAME resolution_nosuchmethod
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${RES_DIR}:${CLASSES_DIR};CallNoMethod"
                -DEXPECT_RC=3
                "-DEXPECT_TEXT=java.lang.NoSuchMethodError" -P "${CHECK}")

    add_test(NAME resolution_nosuchfield
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${RES_DIR}:${CLASSES_DIR};CallNoField"
                -DEXPECT_RC=3
                "-DEXPECT_TEXT=java.lang.NoSuchFieldError" -P "${CHECK}")

    add_test(NAME resolution_noclassdef
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-cp;${RES_DIR}:${CLASSES_DIR};CallNoClass"
                -DEXPECT_RC=3
                "-DEXPECT_TEXT=java.lang.NoClassDefFoundError" -P "${CHECK}")


    # ------------------------------------------------------------------
    # `wide ret` (and jsr) with a local index above 255.  javac no longer
    # emits jsr/ret, so the subroutine method is assembled directly.
    # ------------------------------------------------------------------
    if(PYTHON3_EXECUTABLE)
        # A concrete class whose method table lacks the interface method is
        # produced by clearing ACC_ABSTRACT from an abstract class file.
        add_custom_command(
            OUTPUT "${RES_DIR}/.abstract.stamp"
            COMMAND "${CMAKE_COMMAND}" -E make_directory "${RES_DIR}"
            COMMAND "${JAVAC}" -source 8 -target 8 -d "${RES_DIR}"
                    "${RES_SRC}/CallAbstract.java"
                    "${RES_SRC}/stub/Iface.java" "${RES_SRC}/stub/Impl.java"
            COMMAND "${JAVAC}" -source 8 -target 8 -classpath "${RES_DIR}"
                    -d "${RES_DIR}" "${RES_SRC}/runtime/Impl.java"
            COMMAND "${PYTHON3_EXECUTABLE}"
                    "${RES_SRC}/clear_abstract.py" "${RES_DIR}/Impl.class"
            COMMAND "${CMAKE_COMMAND}" -E touch "${RES_DIR}/.abstract.stamp"
            DEPENDS "${RES_SRC}/CallAbstract.java" "${RES_SRC}/stub/Iface.java"
                    "${RES_SRC}/stub/Impl.java" "${RES_SRC}/runtime/Impl.java"
                    "${RES_SRC}/clear_abstract.py"
            COMMENT "Compiling the AbstractMethodError fixture"
            VERBATIM)
        add_custom_target(resolution_abstract_classes ALL
            DEPENDS "${RES_DIR}/.abstract.stamp")

        add_test(NAME resolution_abstractmethod
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${RES_DIR}:${CLASSES_DIR};CallAbstract"
                    -DEXPECT_RC=3
                    "-DEXPECT_TEXT=java.lang.AbstractMethodError" -P "${CHECK}")

        add_custom_command(
            OUTPUT "${RES_DIR}/.ret.stamp"
            COMMAND "${CMAKE_COMMAND}" -E make_directory "${RES_DIR}"
            COMMAND "${JAVAC}" -source 8 -target 8 -d "${RES_DIR}"
                    "${RES_SRC}/RetCheck.java" "${RES_SRC}/stub/WideRet.java"
            COMMAND "${PYTHON3_EXECUTABLE}" "${RES_SRC}/make_wide_ret.py"
                    "${RES_DIR}/WideRet.class"
            COMMAND "${CMAKE_COMMAND}" -E touch "${RES_DIR}/.ret.stamp"
            DEPENDS "${RES_SRC}/RetCheck.java" "${RES_SRC}/stub/WideRet.java"
                    "${RES_SRC}/make_wide_ret.py"
            COMMENT "Assembling the wide ret fixture"
            VERBATIM)
        add_custom_target(resolution_ret_classes ALL
            DEPENDS "${RES_DIR}/.ret.stamp")

        add_test(NAME wide_ret
            COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                    "-DARGS=-cp;${RES_DIR}:${CLASSES_DIR};RetCheck"
                    -DEXPECT_RC=0 "-DEXPECT_TEXT=wide-ret=2" -P "${CHECK}")
    endif()

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
    add_thread_test(MainMonitorHandoff "main-monitor-handoff ok")
    add_thread_test(TimedWait "timed-wait ok")

    # ------------------------------------------------------------------
    # Deadlock / starvation diagnostics (B4).  The watchdog exits with
    # code 5 for a deadlock and 6 for starvation, so these tests assert
    # both the exit code and the stderr report.
    # ------------------------------------------------------------------
    function(add_deadlock_test name expect2 mp)
        add_test(NAME diag_${name}_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};-Ddarks.diag.deadlock=1;${name}"
                -DEXPECT_RC=5 -DTIMEOUT=120
                "-DEXPECT_TEXT=deadlock detected"
                "-DEXPECT_TEXT2=${expect2}"
                -P "${CHECK}")
    endfunction()

    foreach(mp 1 4)
        add_deadlock_test(MonitorDeadlock "deadlock-a" ${mp})
        add_deadlock_test(JoinCycle "join-a" ${mp})
        add_deadlock_test(ChannelCycle "channel-a" ${mp})
    endforeach()

    add_test(NAME diag_TimedWaitNoFalsePositive
        COMMAND "${CMAKE_COMMAND}"
            "-DPROG=${CMAKE_COMMAND}"
            "-DARGS=-E;env;DARKS_JVM_MP=1;${JVM};-cp;${CLASSES_DIR};-Ddarks.diag.deadlock=1;TimedWaitNoFalsePositive"
            -DEXPECT_RC=0 -DTIMEOUT=120
            "-DEXPECT_TEXT=timed-wait-no-false-positive ok"
            -P "${CHECK}")

    add_test(NAME diag_Starvation_mp1
        COMMAND "${CMAKE_COMMAND}"
            "-DPROG=${CMAKE_COMMAND}"
            "-DARGS=-E;env;DARKS_JVM_MP=1;${JVM};-cp;${CLASSES_DIR};-Ddarks.diag.starvation=1;-Ddarks.diag.starvation.ms=2;Starvation"
            -DEXPECT_RC=6 -DTIMEOUT=180
            "-DEXPECT_TEXT=starvation detected"
            "-DEXPECT_TEXT2=burner-"
            -P "${CHECK}")

    # ------------------------------------------------------------------
    # Virtual threads / minimal java.util.concurrent (combination B, B0+B1).
    # ------------------------------------------------------------------
    if(JAVAC21_MAJOR GREATER_EQUAL 21)
    add_test(NAME virtual_thread_scale
        COMMAND "${CMAKE_COMMAND}"
            "-DPROG=${CMAKE_COMMAND}"
            "-DARGS=-E;env;DARKS_JVM_MP=1;DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};VirtualThreadScale;100000"
            -DEXPECT_RC=0 -DTIMEOUT=600
            "-DEXPECT_TEXT=virtual-thread-scale ok n=100000 virtual=true"
            -P "${CHECK}")

    foreach(mp 1 4)
        add_test(NAME executor_service_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};ExecutorServiceTest;10000"
                -DEXPECT_RC=0 -DTIMEOUT=300
                "-DEXPECT_TEXT=executor-service ok n=10000"
                "-DEXPECT_TEXT2=timeout=true cancel=true terminated=true rejected=true"
                -P "${CHECK}")
    endforeach()

    add_test(NAME virtual_thread_interrupt
        COMMAND "${CMAKE_COMMAND}"
            "-DPROG=${CMAKE_COMMAND}"
            "-DARGS=-E;env;DARKS_JVM_MP=1;DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};VirtualThreadInterruptTest"
            -DEXPECT_RC=0 -DTIMEOUT=120
            "-DEXPECT_TEXT=virtual-thread-interrupt ok virtual=true interrupted=true"
            -P "${CHECK}")

    add_test(NAME virtual_thread_pinning
        COMMAND "${CMAKE_COMMAND}"
            "-DPROG=${CMAKE_COMMAND}"
            "-DARGS=-E;env;DARKS_JVM_MP=1;DARKS_VTHREAD_PINNING=warn;${JVM};-cp;${CLASSES_DIR};PinningTest"
            -DEXPECT_RC=0 -DTIMEOUT=120
            "-DEXPECT_TEXT=pinned in a native call"
            "-DEXPECT_TEXT2=pinning ok virtual=true platform=false"
            -P "${CHECK}")

    # Structured concurrency (combination B, B2): scope policies, deadline,
    # cancellation, leak detection and nested scopes.  MP=1 keeps the
    # live-subtask assertions deterministic.
    add_test(NAME structured_scope
        COMMAND "${CMAKE_COMMAND}"
            "-DPROG=${CMAKE_COMMAND}"
            "-DARGS=-E;env;DARKS_JVM_MP=1;DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};StructuredScopeTest"
            -DEXPECT_RC=0 -DTIMEOUT=180
            "-DEXPECT_TEXT=structured-scope ok"
            -P "${CHECK}")

    add_test(NAME structured_scope_mp4
        COMMAND "${CMAKE_COMMAND}"
            "-DPROG=${CMAKE_COMMAND}"
            "-DARGS=-E;env;DARKS_JVM_MP=4;DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};StructuredScopeTest"
            -DEXPECT_RC=0 -DTIMEOUT=180
            "-DEXPECT_TEXT=structured-scope ok"
            -P "${CHECK}")

    # ------------------------------------------------------------------
    # SDK concurrency containers, executors, locks and atomics.  Each test
    # runs with one and four scheduler threads.
    # ------------------------------------------------------------------
    foreach(mp 1 4)
        add_test(NAME sdk_concurrent_collections_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};ConcurrentCollectionsTest"
                -DEXPECT_RC=0 -DTIMEOUT=300
                "-DEXPECT_TEXT=concurrent-collections ok checks=[0-9]+ map=4000 queue=2000 cow=200"
                -P "${CHECK}")

        add_test(NAME sdk_blocking_queue_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};BlockingQueueTest"
                -DEXPECT_RC=0 -DTIMEOUT=180
                "-DEXPECT_TEXT=blocking-queue ok checks=[0-9]+ abq=0,1,2,3,4 lbq=true sync=42"
                -P "${CHECK}")

        add_test(NAME sdk_executor_pool_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};ExecutorPoolTest"
                -DEXPECT_RC=0 -DTIMEOUT=300
                "-DEXPECT_TEXT=executor-pool ok checks=[0-9]+ fixed=39800 rejected=true cached=20"
                -P "${CHECK}")

        add_test(NAME sdk_scheduled_executor_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};ScheduledExecutorTest"
                -DEXPECT_RC=0 -DTIMEOUT=300
                "-DEXPECT_TEXT=scheduled-executor ok checks=[0-9]+ order=ab cancel=true rate=[0-9]+ delay=[0-9]+"
                -P "${CHECK}")

        add_test(NAME sdk_locks_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};LocksTest"
                -DEXPECT_RC=0 -DTIMEOUT=180
                "-DEXPECT_TEXT=locks ok checks=[0-9]+ lock=2000 cond=true latch=true sem=2"
                -P "${CHECK}")

        add_test(NAME sdk_atomics_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};DARKS_VTHREAD_PINNING=off;${JVM};-cp;${CLASSES_DIR};AtomicsTest"
                -DEXPECT_RC=0 -DTIMEOUT=180
                "-DEXPECT_TEXT=atomics ok checks=[0-9]+ ai=16000 arr=4000 adder=8000"
                -P "${CHECK}")
    endforeach()
    endif()

    # ------------------------------------------------------------------
    # Combination B, B3: darks.concurrent Channel and Select.  All tests run
    # with one and four OS threads; ChannelGC additionally forces frequent
    # collections so parked waiters and channel queues are re-scanned.
    # ------------------------------------------------------------------
    foreach(mp 1 4)
        add_test(NAME channel_pingpong_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};ChannelPingPong"
                -DEXPECT_RC=0 -DTIMEOUT=600
                "-DEXPECT_TEXT=channel-pingpong ok rounds=100000.*fifo=true"
                -P "${CHECK}")
        add_test(NAME channel_close_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};ChannelClose"
                -DEXPECT_RC=0 -DTIMEOUT=180
                "-DEXPECT_TEXT=channel-close ok drain=true senders=true receivers=true"
                -P "${CHECK}")
        add_test(NAME select_fairness_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};SelectFairness"
                -DEXPECT_RC=0 -DTIMEOUT=300
                "-DEXPECT_TEXT=select-fairness ok fifo=true default=true oneWake=true"
                -P "${CHECK}")
        add_test(NAME select_interrupt_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};SelectInterrupt"
                -DEXPECT_RC=0 -DTIMEOUT=180
                "-DEXPECT_TEXT=select-interrupt ok select=true send=true cancelled=true"
                -P "${CHECK}")
        add_test(NAME channel_gc_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};DARKS_JVM_GC_THRESHOLD=512;DARKS_JVM_GC_LOG=1;${JVM};-cp;${CLASSES_DIR};ChannelGC"
                -DEXPECT_RC=0 -DTIMEOUT=300
                "-DEXPECT_TEXT=channel-gc ok live=true parked=true select=true"
                "-DEXPECT_TEXT2=gc-collect"
                -P "${CHECK}")
    endforeach()

    # ------------------------------------------------------------------
    # Combination B, B5: deterministic execution mode.  The virtual clock and
    # seeded scheduling are exercised directly; the repeat/replay drivers
    # launch independent JVMs and compare event hashes.
    # ------------------------------------------------------------------
    add_test(NAME deterministic_virtual_clock
        COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                "-DARGS=-Ddarks.deterministic=5;-cp;${CLASSES_DIR};VirtualClock"
                -DEXPECT_RC=0 -DTIMEOUT=30
                "-DEXPECT_TEXT=virtual-clock ok"
                "-DEXPECT_TEXT2=elapsed_ms=3600000" -P "${CHECK}")

    add_test(NAME deterministic_repeat
        COMMAND "${CMAKE_COMMAND}"
                "-DJVM=${JVM}" "-DCLASSES=${CLASSES_DIR}" "-DREPEAT=100"
                -P "${CMAKE_CURRENT_SOURCE_DIR}/deterministic_repeat.cmake")
    set_tests_properties(deterministic_repeat PROPERTIES TIMEOUT 600)

    add_test(NAME deterministic_race
        COMMAND "${CMAKE_COMMAND}"
                "-DJVM=${JVM}" "-DCLASSES=${CLASSES_DIR}"
                -P "${CMAKE_CURRENT_SOURCE_DIR}/deterministic_race.cmake")
    set_tests_properties(deterministic_race PROPERTIES TIMEOUT 300)

    add_test(NAME deterministic_replay
        COMMAND "${CMAKE_COMMAND}"
                "-DJVM=${JVM}" "-DCLASSES=${CLASSES_DIR}" "-DWORK_DIR=${CMAKE_BINARY_DIR}"
                -P "${CMAKE_CURRENT_SOURCE_DIR}/deterministic_replay.cmake")
    set_tests_properties(deterministic_replay PROPERTIES TIMEOUT 300)

    # ------------------------------------------------------------------
    # Producer/consumer example (examples/producer-consumer) and the
    # synchronized-counter / racy-counter correctness checks.  Sources are
    # compiled with --release 8 (no invokedynamic) and share the test class
    # directory so a plain -cp works on every platform.
    # ------------------------------------------------------------------
    set(PC_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../examples/producer-consumer")
    # Compile into the shared classes directory so the tests can use the same
    # single-entry class path on every platform.
    set(PC_STAMP "${CMAKE_BINARY_DIR}/producer-consumer.stamp")
    add_custom_command(
        OUTPUT "${PC_STAMP}"
        COMMAND "${CMAKE_COMMAND}" -E make_directory "${CLASSES_DIR}"
        COMMAND "${JAVAC}" --release 8 -d "${CLASSES_DIR}"
                "${PC_SRC_DIR}/ProducerConsumerDemo.java"
                "${PC_SRC_DIR}/CounterCorrectness.java"
        COMMAND "${CMAKE_COMMAND}" -E touch "${PC_STAMP}"
        DEPENDS
            "${PC_SRC_DIR}/ProducerConsumerDemo.java"
            "${PC_SRC_DIR}/CounterCorrectness.java"
        COMMENT "Compiling the producer/consumer example")
    add_custom_target(producer_consumer_classes ALL DEPENDS "${PC_STAMP}")
    add_dependencies(producer_consumer_classes java_classes)

    foreach(mp 1 4)
        add_test(NAME producer_consumer_e2e_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};ProducerConsumerDemo"
                -DEXPECT_RC=0 -DTIMEOUT=120
                "-DEXPECT_TEXT=producer-consumer ok"
                "-DEXPECT_TEXT2=produced=600 consumed=600 unique=600 duplicates=0 missing=0 outOfRange=0 left=0"
                "-DEXPECT_TEXT3=queueEmpty=true" -P "${CHECK}")
        add_test(NAME concurrency_correctness_mp${mp}
            COMMAND "${CMAKE_COMMAND}"
                "-DPROG=${CMAKE_COMMAND}"
                "-DARGS=-E;env;DARKS_JVM_MP=${mp};${JVM};-cp;${CLASSES_DIR};CounterCorrectness"
                -DEXPECT_RC=0 -DTIMEOUT=120
                "-DEXPECT_TEXT=counter-check ok locked=100000 expected=100000"
                -P "${CHECK}")
    endforeach()

    # Stability: run the same demo 20 times, alternating MP=1 and MP=4.
    add_test(NAME producer_consumer_repeat
        COMMAND "${CMAKE_COMMAND}"
            "-DJVM=${JVM}" "-DCLASSES=${CLASSES_DIR}" "-DREPEAT=20"
            -P "${CMAKE_CURRENT_SOURCE_DIR}/producer_consumer_repeat.cmake")
    set_tests_properties(producer_consumer_repeat PROPERTIES TIMEOUT 300)

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

        # Opt-in smoke test for the real, unmodified Spring Boot 2.7.18
        # launcher: point DARKS_SPRING_PROBE_JAR at the application jar (the
        # jar must contain its own runtime classes or use a manifest
        # Class-Path; the VM's runtime classes come from the build tree).
        if(DEFINED ENV{DARKS_SPRING_PROBE_JAR} AND EXISTS "$ENV{DARKS_SPRING_PROBE_JAR}")
            add_test(NAME spring_boot_loader_probe
                COMMAND "${CMAKE_COMMAND}" -DPROG=${JVM}
                        "-DARGS=-cp;${CLASSES_DIR};-jar;$ENV{DARKS_SPRING_PROBE_JAR}"
                        -DEXPECT_RC=0 "-DEXPECT_TEXT=PROBE-OK"
                        -DTIMEOUT=900 -P "${CHECK}")
        endif()
    endif()

else()
    message(WARNING "javac not found: end-to-end tests are disabled.")
    add_test(NAME unit_tests_without_classes
        COMMAND "${CMAKE_COMMAND}" -DPROG=$<TARGET_FILE:unit_tests>
                -DEXPECT_RC=0 -DEXPECT_TEXT=skipped -P "${CMAKE_CURRENT_SOURCE_DIR}/check.cmake")
endif()
