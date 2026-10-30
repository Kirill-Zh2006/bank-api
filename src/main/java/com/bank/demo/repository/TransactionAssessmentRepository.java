package com.bank.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bank.demo.entity.TransactionAssessment;
import com.bank.demo.repository.projection.ExceededTransactionProjection;

public interface TransactionAssessmentRepository extends JpaRepository<TransactionAssessment, Long>{

    @Query(value = """
    WITH latest_applicable_limit AS (
        SELECT
            transaction_row.id AS transaction_id,
            MAX(candidate_limit.effective_at) AS effective_at
        FROM expense_transaction transaction_row
        JOIN spending_limit candidate_limit
            ON candidate_limit.budget_scope_id = transaction_row.budget_scope_id
           AND candidate_limit.effective_at <= transaction_row.occurred_at
        GROUP BY transaction_row.id
    ),
    transactions_with_limits AS (
        SELECT
            ca.account_from AS account_from,
            et.account_to AS account_to,
            et.currency_code AS currency_shortname,
            et.amount_original AS original_sum,
            bs.category AS expense_category,
            et.occurred_at AS occurred_at,
            et.id AS transaction_id,
            bs.id AS budget_scope_id,
            ta.amount_usd AS amount_usd,
            sl.effective_at AS limit_date,
            COALESCE(sl.amount_usd, 1000.00) AS limit_sum,
            SUM(ta.amount_usd) OVER (
                PARTITION BY
                    bs.id,
                    date_trunc('month', et.occurred_at AT TIME ZONE 'UTC')
                ORDER BY et.occurred_at, et.id
                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
            ) AS cumulative_spend_usd
        FROM transaction_assessment ta
        JOIN expense_transaction et
            ON et.id = ta.transaction_id
        JOIN budget_scope bs
            ON bs.id = et.budget_scope_id
        JOIN customer_account ca
            ON ca.id = bs.account_id
        LEFT JOIN latest_applicable_limit latest_limit
            ON latest_limit.transaction_id = et.id
        LEFT JOIN spending_limit sl
            ON sl.budget_scope_id = et.budget_scope_id
           AND sl.effective_at = latest_limit.effective_at
        WHERE ca.account_from = :accountFrom
          AND bs.category = :category
    )
    SELECT
        account_from AS "accountFrom",
        account_to AS "accountTo",
        currency_shortname AS "currencyShortname",
        original_sum AS "sum",
        expense_category AS "expenseCategory",
        occurred_at AS "datetime",
        limit_date AS "limitDate",
        limit_sum AS "limitSum",
        'USD' AS "limitCurrencyShortname"
    FROM transactions_with_limits
    WHERE cumulative_spend_usd > limit_sum
    ORDER BY occurred_at DESC, transaction_id DESC
    """, nativeQuery = true)
List<ExceededTransactionProjection> findExceededTransactions(
        @Param("accountFrom") String accountFrom,
        @Param("category") String category);
}
