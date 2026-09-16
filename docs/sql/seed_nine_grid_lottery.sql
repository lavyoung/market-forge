USE `market-forge-001`;

SELECT strategy_id,
       award_id,
       award_title,
       award_count_surplus,
       award_rate,
       sort
FROM strategy_award
WHERE strategy_id = 900901001
ORDER BY sort;