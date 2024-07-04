#!/bin/sh -l

time=$(date)
echo "time=$time" >> "$GITHUB_OUTPUT"

./minio server /s3 --console-address :9001
