#!/bin/bash

PORT=8080

exec java -jar -Dserver.port="${PORT}" "refund-request-consumer-java.jar"