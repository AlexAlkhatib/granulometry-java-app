#!/usr/bin/env bash
# Lance Granulometrie Studio depuis le paquet construit (target/).
set -euo pipefail
cd "$(dirname "$0")"

if command -v mvn >/dev/null 2>&1 && [ ! -f target/granulometrie-studio.jar ]; then
  echo "Construction de l'application (mvn package)…"
  mvn -q -DskipTests package
fi

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA="java"
fi

exec "$JAVA" \
  --module-path target/lib/javafx \
  --add-modules javafx.controls,javafx.swing \
  -cp "target/granulometrie-studio.jar:target/lib/h2-2.2.224.jar" \
  fr.granulometrie.Launcher "$@"
