#!/bin/zsh
set -euo pipefail

REPO_ROOT="/Users/alfafermer/IdeaProjects/devicehub"
NODE_BIN="${NODE_BIN:-/opt/homebrew/bin/node}"

if [[ ! -x "${NODE_BIN}" ]]; then
  NODE_BIN="/usr/local/bin/node"
fi

if [[ ! -x "${NODE_BIN}" ]]; then
  echo "node binary not found. Set NODE_BIN explicitly." >&2
  exit 1
fi

cd "${REPO_ROOT}"

export MONGODB_PORT_27017_TCP='mongodb://192.168.0.123:32017/?directConnection=true'

exec "${NODE_BIN}" ./.build/bin/stf.mjs ios-provider \
  --connect-sub tcp://192.168.0.121:31250 \
  --connect-push tcp://192.168.0.121:31270 \
  --screen-ws-url-pattern 'wss://devicehub.putmyhexon.ru/d/ios-provider/<%= publicPort %>/' \
  --public-ip devicehub.putmyhexon.ru \
  --provider ios-provider \
  --storage-url http://192.168.0.121:31300/ \
  --secret nosecret \
  --host 192.168.0.104 \
  --no-cleanup
