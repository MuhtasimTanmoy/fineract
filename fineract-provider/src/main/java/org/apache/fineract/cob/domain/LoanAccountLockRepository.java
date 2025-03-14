/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cob.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LoanAccountLockRepository extends JpaRepository<LoanAccountLock, Long>, JpaSpecificationExecutor<LoanAccountLock> {

    Optional<LoanAccountLock> findByLoanIdAndLockOwner(Long loanId, LockOwner lockOwner);

    void deleteByLoanIdInAndLockOwner(List<Long> loanIds, LockOwner lockOwner);

    List<LoanAccountLock> findAllByLoanIdIn(List<Long> loanIds);

    boolean existsByLoanIdAndLockOwner(Long loanId, LockOwner lockOwner);

    @Query(value = """
                                                 update m_loan set last_closed_business_date = (select lck.lock_placed_on_cob_business_date - 1
                                                 from m_loan_account_locks lck
                                                 where lck.loan_id = id
                                                   and lck.lock_placed_on_cob_business_date is not null
                                                   and lck.error is not null
                                                   and lck.lock_owner in ('LOAN_COB_CHUNK_PROCESSING','LOAN_INLINE_COB_PROCESSING'))
            where last_closed_business_date is null and exists  (select lck.loan_id
                          from m_loan_account_locks lck  where lck.loan_id = id
                            and lck.lock_placed_on_cob_business_date is not null and lck.error is not null
                            and lck.lock_owner in ('LOAN_COB_CHUNK_PROCESSING','LOAN_INLINE_COB_PROCESSING'))""", nativeQuery = true)
    @Modifying(flushAutomatically = true)
    void updateLoanFromAccountLocks();

    @Query("""
            update LoanAccountLock lck set
            lck.error = null, lck.lockOwner=org.apache.fineract.cob.domain.LockOwner.LOAN_COB_PARTITIONING
            where lck.lockPlacedOnCobBusinessDate is not null and lck.error is not null and
            lck.lockOwner in (org.apache.fineract.cob.domain.LockOwner.LOAN_COB_CHUNK_PROCESSING,org.apache.fineract.cob.domain.LockOwner.LOAN_INLINE_COB_PROCESSING)
            """)
    @Modifying(flushAutomatically = true)
    void updateToSoftLockByOwner();
}
