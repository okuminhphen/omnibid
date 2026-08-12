package com.omnibid.audit.repository;

import com.omnibid.audit.domain.BidLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface BidLogRepository extends MongoRepository<BidLog, UUID> {
}
