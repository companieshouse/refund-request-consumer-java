#!/bin/bash

PORT=808

exec java -jar -Dserver.port="${PORT}" "refund-request-consumer-java.jar"