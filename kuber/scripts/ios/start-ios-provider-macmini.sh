#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/ios-provider.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing env file: ${ENV_FILE}" >&2
  echo "Create it from ios-provider.env.example and run again." >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

# Required values from ios-provider.env
: "${MAC_MINI_IP:?MAC_MINI_IP is required in ios-provider.env}"
: "${K3S_NODE_IP:?K3S_NODE_IP is required in ios-provider.env}"
: "${STF_SECRET:?STF_SECRET is required in ios-provider.env}"
: "${IOS_PROVIDER_NAME:?IOS_PROVIDER_NAME is required in ios-provider.env}"

# Optional values from ios-provider.env
IOS_SCREEN_WS_URL_PATTERN="${IOS_SCREEN_WS_URL_PATTERN:-ws://${MAC_MINI_IP}:<%= publicPort %>}"
IOS_PORT_RANGE_MIN="${IOS_PORT_RANGE_MIN:-8100}"
IOS_PORT_RANGE_MAX="${IOS_PORT_RANGE_MAX:-8200}"
IOS_SCREEN_WS_RANGE_MIN="${IOS_SCREEN_WS_RANGE_MIN:-18000}"
IOS_SCREEN_WS_RANGE_MAX="${IOS_SCREEN_WS_RANGE_MAX:-18100}"
IOS_WDA_RANGE_MIN="${IOS_WDA_RANGE_MIN:-18200}"
IOS_WDA_RANGE_MAX="${IOS_WDA_RANGE_MAX:-18300}"
IOS_SERIALS="${IOS_SERIALS:-}"

CONNECT_SUB="tcp://${K3S_NODE_IP}:31250"
CONNECT_PUSH="tcp://${K3S_NODE_IP}:31270"
STORAGE_URL="http://${K3S_NODE_IP}:31300/"

declare -a IOS_SERIAL_ARRAY=()
if [[ -n "${IOS_SERIALS}" ]]; then
  # ios-provider.env format: IOS_SERIALS="udid-1,udid-2"
  IFS=',' read -r -a IOS_SERIAL_ARRAY <<<"${IOS_SERIALS}"
fi

ARGS=(
  ios-provider
  --provider "${IOS_PROVIDER_NAME}"
  --public-ip "${MAC_MINI_IP}"
  --host "${MAC_MINI_IP}"
  --screen-ws-url-pattern "${IOS_SCREEN_WS_URL_PATTERN}"
  --storage-url "${STORAGE_URL}"
  --connect-sub "${CONNECT_SUB}"
  --connect-push "${CONNECT_PUSH}"
  --secret "${STF_SECRET}"
  --port-range-min "${IOS_PORT_RANGE_MIN}"
  --port-range-max "${IOS_PORT_RANGE_MAX}"
  --screen-ws-range-min "${IOS_SCREEN_WS_RANGE_MIN}"
  --screen-ws-range-max "${IOS_SCREEN_WS_RANGE_MAX}"
  --wda-range-min "${IOS_WDA_RANGE_MIN}"
  --wda-range-max "${IOS_WDA_RANGE_MAX}"
)

for serial in "${IOS_SERIAL_ARRAY[@]}"; do
  trimmed="$(echo "${serial}" | xargs)"
  if [[ -n "${trimmed}" ]]; then
    ARGS+=(--serial "${trimmed}")
  fi
done

echo "Starting iOS provider:"
echo "  provider=${IOS_PROVIDER_NAME}"
echo "  connect-sub=${CONNECT_SUB}"
echo "  connect-push=${CONNECT_PUSH}"
echo "  storage-url=${STORAGE_URL}"
echo "  public-ip=${MAC_MINI_IP}"

stf "${ARGS[@]}"
