#!/bin/bash

# Build, run and check CAS authentication for jooby-pac4j-demo
# Usage: ./run_and_check.sh

set -e

cd ..

mkdir -p target

# Build demo
echo "📦 Building jooby-pac4j-demo..."
mvn -q clean package

# Locate the shaded jar (version-independent)
JAR=$(ls target/jooby-pac4j-demo-*.jar | head -n1)
if [ -z "$JAR" ]; then
  echo "❌ Shaded jar not found in target/"
  exit 1
fi

# Start server
echo "🌐 Starting server ($JAR)..."
java -jar "$JAR" > target/server.log 2>&1 &
SERVER_PID=$!

# Wait for server to start (max 60s)
echo "⏳ Waiting for server startup..."
for i in {1..60}; do
  if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200"; then
    echo "✅ Server started"
    break
  fi
  if [ $i -eq 60 ]; then
    echo "❌ Timeout waiting for server"
    echo "📋 Server logs:"
    cat target/server.log || true
    kill $SERVER_PID 2>/dev/null || true
    exit 1
  fi
  sleep 1
done

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080)
if [ "$HTTP_CODE" != "200" ]; then
  echo "❌ Root not 200 (got $HTTP_CODE)"
  kill $SERVER_PID 2>/dev/null || true
  exit 1
fi

echo "🔗 Testing CAS redirection and login..."
CASLINK_URL="http://localhost:8080/cas/index"
echo "📍 Following $CASLINK_URL"

CAS_RESPONSE=$(curl -s -L -w "FINAL_URL:%{url_effective}\nHTTP_CODE:%{http_code}" "$CASLINK_URL")
CAS_HTTP_CODE=$(echo "$CAS_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
CAS_FINAL_URL=$(echo "$CAS_RESPONSE" | grep "FINAL_URL:" | cut -d: -f2-)
CAS_CONTENT=$(echo "$CAS_RESPONSE" | sed '/^FINAL_URL:/d' | sed '/^HTTP_CODE:/d')

echo "🌐 Final URL: $CAS_FINAL_URL"
echo "📄 HTTP Code: $CAS_HTTP_CODE"

if [ "$CAS_HTTP_CODE" = "200" ] && echo "$CAS_CONTENT" | grep -q "Enter Username & Password"; then
  echo "✅ Reached CAS login page"
  CAS_TEST_PASSED=true
else
  echo "❌ CAS login page test failed"
  CAS_TEST_PASSED=false
fi

CAS_AUTH_PASSED=false
if [ "$CAS_TEST_PASSED" = true ]; then
  echo "🧪 Simulating CAS login via curl..."
  COOKIE_JAR="target/cas_cookies.txt"
  CAS_LOGIN_PAGE="target/cas_login.html"
  CAS_AFTER_LOGIN="target/cas_after_login.html"
  FINAL_APP_PAGE="target/final_app.html"

  curl -s -c "$COOKIE_JAR" -b "$COOKIE_JAR" -L "$CAS_FINAL_URL" -o "$CAS_LOGIN_PAGE" -w "FINAL_URL:%{url_effective}\nHTTP_CODE:%{http_code}\n" > target/cas_login_fetch.meta
  EXECUTION=$(grep -Eo 'name=\"execution\"[^>]*value=\"[^\"]+\"' "$CAS_LOGIN_PAGE" | sed -E 's/.*value=\"([^\"]+)\".*/\1/' | head -n1 || true)

  if [ -z "$EXECUTION" ]; then
    echo "❌ Could not extract CAS execution token"
  else
    CAS_POST_RESPONSE=$(curl -s -c "$COOKIE_JAR" -b "$COOKIE_JAR" -L -o "$CAS_AFTER_LOGIN" -w "FINAL_URL:%{url_effective}\nHTTP_CODE:%{http_code}" \
      --data-urlencode "username=leleuj@gmail.com" \
      --data-urlencode "password=password" \
      --data-urlencode "execution=$EXECUTION" \
      --data-urlencode "_eventId=submit" \
      "$CAS_FINAL_URL")

    CAS_POST_FINAL_URL=$(echo "$CAS_POST_RESPONSE" | grep "FINAL_URL:" | cut -d: -f2-)
    echo "🌐 After login redirect URL: $CAS_POST_FINAL_URL"

    # After successful CAS login, navigate to a CAS-protected page to verify identity
    TARGET_URL="http://localhost:8080/cas/index"
    FINAL_META=$(curl -s -c "$COOKIE_JAR" -b "$COOKIE_JAR" -L -o "$FINAL_APP_PAGE" -w "FINAL_URL:%{url_effective}\nHTTP_CODE:%{http_code}" "$TARGET_URL")
    FINAL_APP_CODE=$(echo "$FINAL_META" | grep "HTTP_CODE:" | cut -d: -f2)

    if [ "$FINAL_APP_CODE" = "200" ]; then
      echo "✅ Demo reachable after CAS login (HTTP 200)"
      if grep -q "leleuj@gmail.com" "$FINAL_APP_PAGE"; then
        echo "✅ Authenticated identity found in final page"
        CAS_AUTH_PASSED=true
      else
        echo "❌ Authenticated identity not found in protected page"
        CAS_AUTH_PASSED=false
      fi
    else
      echo "❌ Demo not reachable after CAS login (HTTP $FINAL_APP_CODE)"
      CAS_AUTH_PASSED=false
    fi
  fi
fi

echo "🛑 Stopping server..."
kill $SERVER_PID 2>/dev/null || true
sleep 2
kill -9 $SERVER_PID 2>/dev/null || true

if [ "$CAS_TEST_PASSED" = true ] && [ "$CAS_AUTH_PASSED" = true ]; then
  echo "🎉 jooby-pac4j-demo CAS test passed"
  exit 0
else
  echo "💥 jooby-pac4j-demo CAS test failed"
  [ "$CAS_TEST_PASSED" = true ] || echo "❌ CAS redirection test failed"
  [ "$CAS_AUTH_PASSED" = true ] || echo "❌ CAS authentication flow failed"
  exit 1
fi
