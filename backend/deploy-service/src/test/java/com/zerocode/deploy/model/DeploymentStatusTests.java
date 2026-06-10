package com.zerocode.deploy.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeploymentStatusTests {

    @Test
    void exposesStableApiValues() {
        assertThat(DeploymentStatus.PLANNED.value()).isEqualTo("planned");
        assertThat(DeploymentStatus.RUNNING.value()).isEqualTo("running");
        assertThat(DeploymentStatus.SUCCEEDED.value()).isEqualTo("succeeded");
        assertThat(DeploymentStatus.FAILED.value()).isEqualTo("failed");
        assertThat(DeploymentStatus.SKIPPED.value()).isEqualTo("skipped");
    }
}
