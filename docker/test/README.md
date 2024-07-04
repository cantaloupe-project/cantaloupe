# Test MEMO

## how to use
```text
docker-compose -f docker/test/docker-compose.yml -p cantaloupe up --build -d
docker exec TestCantaloupe /bin/sh -c "mvn clean test -Pfreedeps"

# ping
docker exec TestCantaloupe /bin/sh -c "curl 'http://minio-host:9001'"
docker exec TestCantaloupe /bin/sh -c "redis-cli ping"

# ps
docker exec TestCantaloupe /bin/sh -c "ps aux |grep minio"
docker exec TestCantaloupe /bin/sh -c "ps aux |grep redis"
```
check [ci.yml](..%2F..%2F.github%2Fworkflows%2Fci.yml)

## for mac
```text
docker pull --platform linux/arm64 openjdk:11
or
docker pull --platform linux/arm64/v8 openjdk:11
```

### extra
```text
mvn clean test -Pnodeps
mvn clean test -Ponlydeps
mvn clean test -Pfreedeps
```

## issue
- resolved USERの扱い
  - github actionsはdockerfile内でUSERを使うことを推奨していない。
    - [https://docs.github.com/ja/actions/creating-actions/dockerfile-support-for-github-actions#user](https://docs.github.com/ja/actions/creating-actions/dockerfile-support-for-github-actions#user)
  - 一部のテストはユーザ設定をしないとパスできない（らしい）
    - dockerfileには記載と設定があった
      - `# A non-root user is needed for some FilesystemSourceTest tests to work.`

- jdk16
  - PID-1225
