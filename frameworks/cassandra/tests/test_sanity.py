import pytest
import sdk_hosts
import sdk_install
import sdk_jobs
import sdk_metrics
import sdk_networks
import sdk_plan
import sdk_upgrade
import sdk_utils

from tests import config


@pytest.fixture(scope="module", autouse=True)
def configure_package(configure_security):
    test_jobs = []
    try:
        test_jobs = config.get_all_jobs(node_address=config.get_foldered_node_address())
        # destroy/reinstall any prior leftover jobs, so that they don't touch the newly installed service:
        for job in test_jobs:
            sdk_jobs.install_job(job)

        sdk_install.uninstall(config.PACKAGE_NAME, config.get_foldered_service_name())
        sdk_upgrade.test_upgrade(
            config.PACKAGE_NAME,
            config.get_foldered_service_name(),
            config.DEFAULT_TASK_COUNT,
            additional_options={"service": {"name": config.get_foldered_service_name()}},
        )

        yield  # let the test session execute
    finally:
        sdk_install.uninstall(config.PACKAGE_NAME, config.get_foldered_service_name())

        for job in test_jobs:
            sdk_jobs.remove_job(job)


@pytest.mark.sanity
def test_endpoints():
    # check that we can reach the scheduler via admin router, and that returned endpoints are sanitized:
    endpoints = sdk_networks.get_endpoint(
        config.PACKAGE_NAME, config.get_foldered_service_name(), "native-client"
    )
    assert endpoints["dns"][0] == sdk_hosts.autoip_host(
        config.get_foldered_service_name(), "node-0-server", 9042
    )
    assert "vip" not in endpoints


@pytest.mark.sanity
@pytest.mark.smoke
def test_repair_cleanup_plans_complete():
    parameters = {"CASSANDRA_KEYSPACE": "testspace1"}

    # populate 'testspace1' for test, then delete afterwards:
    with sdk_jobs.RunJobContext(
        before_jobs=[
            config.get_write_data_job(node_address=config.get_foldered_node_address()),
            config.get_verify_data_job(node_address=config.get_foldered_node_address()),
        ],
        after_jobs=[
            config.get_delete_data_job(node_address=config.get_foldered_node_address()),
            config.get_verify_deletion_job(node_address=config.get_foldered_node_address()),
        ],
    ):

        sdk_plan.start_plan(config.get_foldered_service_name(), "cleanup", parameters=parameters)
        sdk_plan.wait_for_completed_plan(config.get_foldered_service_name(), "cleanup")

        sdk_plan.start_plan(config.get_foldered_service_name(), "repair", parameters=parameters)
        sdk_plan.wait_for_completed_plan(config.get_foldered_service_name(), "repair")


@pytest.mark.sanity
@pytest.mark.metrics
@pytest.mark.dcos_min_version("1.9")
@pytest.mark.skipif(
    sdk_utils.dcos_version_at_least("1.12"),
    reason="Metrics are not working on 1.12. Reenable once this is fixed",
)
def test_metrics():
    expected_metrics = [
        "org.apache.cassandra.metrics.Table.CoordinatorReadLatency.system.hints.p999",
        "org.apache.cassandra.metrics.Table.CompressionRatio.system_schema.indexes",
        "org.apache.cassandra.metrics.ThreadPools.ActiveTasks.internal.MemtableReclaimMemory",
    ]

    def expected_metrics_exist(emitted_metrics):
        return sdk_metrics.check_metrics_presence(emitted_metrics, expected_metrics)

    sdk_metrics.wait_for_service_metrics(
        config.PACKAGE_NAME,
        config.get_foldered_service_name(),
        "node-0",
        "node-0-server",
        config.DEFAULT_CASSANDRA_TIMEOUT,
        expected_metrics_exist,
    )
