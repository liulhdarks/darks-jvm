#!/usr/bin/env bash
# Build Spring PetClinic 2.7.3 with a JDK 11 and run it on darks-jvm.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"

WORK_DIR="${WORK_DIR:-$HERE/work}"
PORT="${PORT:-8090}"
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-1800}"
JAVA11_HOME="${JAVA11_HOME:-/usr/lib/jvm/java-11-openjdk-amd64}"
DARKS_JVM="${DARKS_JVM:-$ROOT/build/bin/darks-jvm}"
PETCLINIC_REPO="${PETCLINIC_REPO:-https://github.com/spring-projects/spring-petclinic.git}"
# Last commit that still builds PetClinic 2.7.3 on Spring Boot 2.7.3.
PETCLINIC_COMMIT="${PETCLINIC_COMMIT:-9ecdc1111e3da388a750ace41a125287d9620534}"

log() { printf '== %s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

[ -x "$DARKS_JVM" ] || die "darks-jvm not found at $DARKS_JVM (build it first)"
[ -x "$JAVA11_HOME/bin/java" ] && [ -x "$JAVA11_HOME/bin/javac" ] ||
	die "JDK not found at $JAVA11_HOME (set JAVA11_HOME to a JDK 11)"
"$JAVA11_HOME/bin/java" -version 2>&1 | grep -q '"11\.' ||
	die "$JAVA11_HOME is not JDK 11 ($("$JAVA11_HOME/bin/java" -version 2>&1 | head -1))"
command -v mvn >/dev/null || die "maven (mvn) not found"
command -v git >/dev/null || die "git not found"
command -v curl >/dev/null || die "curl not found"

export JAVA_HOME="$JAVA11_HOME"
mkdir -p "$WORK_DIR"
SRC="$WORK_DIR/spring-petclinic"
BUILD_LOG="$WORK_DIR/build.log"
RUN_LOG="$WORK_DIR/darks-jvm.log"

if [ ! -d "$SRC/.git" ]; then
	log "cloning spring-petclinic"
	git clone -q "$PETCLINIC_REPO" "$SRC" || die "git clone failed"
fi
log "checking out $PETCLINIC_COMMIT"
git -C "$SRC" checkout -q "$PETCLINIC_COMMIT" || die "cannot check out $PETCLINIC_COMMIT"

log "building with JDK 11 (log: $BUILD_LOG)"
if ! (cd "$SRC" && mvn -q -DskipTests package) >"$BUILD_LOG" 2>&1; then
	tail -20 "$BUILD_LOG" >&2
	die "maven build failed"
fi
JAR="$(find "$SRC/target" -maxdepth 1 -name 'spring-petclinic-*.jar' ! -name '*-sources.jar' -print -quit)"
[ -n "$JAR" ] || die "no spring-petclinic jar in $SRC/target"

log "starting darks-jvm on port $PORT (log: $RUN_LOG)"
: >"$RUN_LOG"
"$DARKS_JVM" -cp "$ROOT/build/classes" -jar "$JAR" --server.port="$PORT" >"$RUN_LOG" 2>&1 &
JVM_PID=$!
cleanup() {
	kill "$JVM_PID" 2>/dev/null || true
	wait "$JVM_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

BASE="http://127.0.0.1:$PORT"
deadline=$((SECONDS + STARTUP_TIMEOUT))
log "waiting up to ${STARTUP_TIMEOUT}s for $BASE"
until curl -fsS --max-time 60 -o /dev/null "$BASE/" 2>/dev/null; do
	kill -0 "$JVM_PID" 2>/dev/null || { tail -20 "$RUN_LOG" >&2; die "darks-jvm exited before serving (log: $RUN_LOG)"; }
	[ "$SECONDS" -lt "$deadline" ] || die "startup timeout (log: $RUN_LOG)"
	sleep 5
done

check() {
	local path="$1" expected="$2" body
	body="$(curl -fsS --max-time 120 "$BASE$path")" || { tail -20 "$RUN_LOG" >&2; die "GET $path failed (log: $RUN_LOG)"; }
	printf '%s' "$body" | grep -q "$expected" || die "GET $path missing '$expected' (log: $RUN_LOG)"
	log "GET $path -> 200, contains '$expected'"
}

check "/" "Welcome"
check "/owners/find" "Find Owners"
check "/owners/2" "Owner Information"
check "/owners/2" "Betty Davis"

log "petclinic smoke checks passed"
