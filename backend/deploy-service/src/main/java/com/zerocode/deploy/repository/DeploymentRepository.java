package com.zerocode.deploy.repository;

import com.zerocode.deploy.model.DeploymentRecord;
import java.util.Optional;

public interface DeploymentRepository {
    DeploymentRecord save(DeploymentRecord deployment);

    Optional<DeploymentRecord> findById(String id);
}
