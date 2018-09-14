#!/usr/bin/env bash

set -eu -o pipefail

function usage () {
  echo 'Usage: ./create_service_diagnostics_bundle.sh <package name> <service name> [<proceed>]'
  echo
  echo 'Example: ./create_service_diagnostics_bundle.sh cassandra /prod/cassandra yes'
}

if [ "${#}" -lt 2 ] || [ "${#}" -gt 3 ]; then
  echo -e "create_service_diagnostics_bundle.sh takes either 2 or 3 arguments but was given ${#}\\n"
  usage
  exit 1
fi

readonly REQUIREMENTS='docker'

for requirement in ${REQUIREMENTS}; do
  if ! [[ -x $(command -v "${requirement}") ]]; then
    echo "You need to install '${requirement}' to run this script"
    exit 1
  fi
done

readonly VERSION='v0.1.0'
readonly DCOS_COMMONS_DIRECTORY="/dcos-commons-dist"
readonly DOCKER_IMAGE="mpereira/dcos-commons:diagnostics-${VERSION}"
readonly SCRIPT_PATH="${DCOS_COMMONS_DIRECTORY}/tools/create_service_diagnostics_bundle.py"

readonly PACKAGE_NAME="${1:-}"
readonly SERVICE_NAME="${2:-}"
readonly PROCEED="${3:-}"

readonly BUNDLES_DIRECTORY="service-diagnostic-bundles"
readonly CONTAINER_BUNDLES_DIRECTORY="/${BUNDLES_DIRECTORY}"

mkdir -p "${BUNDLES_DIRECTORY}"

# TODO: handle PROCEED.

docker run \
       -it \
       -v "$(pwd)/${BUNDLES_DIRECTORY}:${CONTAINER_BUNDLES_DIRECTORY}" \
       -v "${HOME}/.dcos":/root/.dcos \
       "${DOCKER_IMAGE}" \
       bash -c "PYTHONPATH=${DCOS_COMMONS_DIRECTORY}/testing ./${SCRIPT_PATH} \
                  --package-name ${PACKAGE_NAME} \
                  --service-name ${SERVICE_NAME} \
                  --bundles-directory ${CONTAINER_BUNDLES_DIRECTORY}"