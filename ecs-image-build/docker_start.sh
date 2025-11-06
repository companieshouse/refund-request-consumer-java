#!/bin/bash

PORT=8081

exec java -jar -Dserver.port="${PORT}" "refund-request-consumer-java.jar"