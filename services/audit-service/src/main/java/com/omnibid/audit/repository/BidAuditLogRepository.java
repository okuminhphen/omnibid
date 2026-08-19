package com.omnibid.audit.repository;

import com.omnibid.audit.domain.BidAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BidAuditLogRepository extends MongoRepository<BidAuditLog, String> {
}
