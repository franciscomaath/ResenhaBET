# Migration Validation Checklist

Run these after applying `V99__LEGACY_DATA_MIGRATION.sql`.

## 1. Core row counts

```sql
SELECT 'groups' AS table_name, COUNT(*) AS actual, 1 AS expected FROM resenha.groups
UNION ALL SELECT 'users', COUNT(*), 18 FROM resenha.users
UNION ALL SELECT 'wallet', COUNT(*), 18 FROM resenha.wallet
UNION ALL SELECT 'player', COUNT(*), 18 FROM resenha.player
UNION ALL SELECT 'group_member', COUNT(*), 18 FROM resenha.group_member
UNION ALL SELECT 'tournament', COUNT(*), 4 FROM resenha.tournament
UNION ALL SELECT 'tournament_player', COUNT(*), 72 FROM resenha.tournament_player
UNION ALL SELECT 'group_tournament', COUNT(*), 4 FROM resenha.group_tournament
UNION ALL SELECT 'group_tournament_market_type', COUNT(*), 4 FROM resenha.group_tournament_market_type
UNION ALL SELECT 'tournament_wallet', COUNT(*), 72 FROM resenha.tournament_wallet
UNION ALL SELECT 'event', COUNT(*), 47 FROM resenha.event
UNION ALL SELECT 'market', COUNT(*), 47 FROM resenha.market;
```

## 2. Betting data counts

```sql
SELECT COUNT(*) AS bet_slips FROM resenha.bet_slip;
SELECT COUNT(*) AS bet_slip_items FROM resenha.bet_slip_item;
SELECT COUNT(*) AS transactions FROM resenha.transaction;
SELECT COUNT(*) AS outcomes FROM resenha.outcome;
SELECT COUNT(*) AS rounds FROM resenha.tournament_round;
```

## 3. Wallet spot checks

```sql
SELECT u.name, w.balance
FROM resenha.users u
JOIN resenha.wallet w ON w.user_id = u.id
WHERE u.name IN ('Francisco', 'Alexandre', 'Nicolas')
ORDER BY u.name;
```

## 4. FK integrity checks

```sql
SELECT COUNT(*) AS orphan_players
FROM resenha.player p
LEFT JOIN resenha.users u ON u.id = p.user_id
LEFT JOIN resenha.groups g ON g.id = p.group_id
WHERE u.id IS NULL OR g.id IS NULL;

SELECT COUNT(*) AS orphan_group_members
FROM resenha.group_member gm
LEFT JOIN resenha.users u ON u.id = gm.user_id
LEFT JOIN resenha.groups g ON g.id = gm.group_id
WHERE u.id IS NULL OR g.id IS NULL;

SELECT COUNT(*) AS orphan_bet_slips
FROM resenha.bet_slip bs
LEFT JOIN resenha.users u ON u.id = bs.user_id
LEFT JOIN resenha.group_tournament gt ON gt.id = bs.group_tournament_id
WHERE u.id IS NULL OR gt.id IS NULL;

SELECT COUNT(*) AS orphan_transactions
FROM resenha.transaction t
LEFT JOIN resenha.bet_slip bs ON bs.id = t.bet_slip_id
LEFT JOIN resenha.tournament_wallet tw ON tw.id = t.tournament_wallet_id
WHERE bs.id IS NULL OR tw.id IS NULL;

SELECT COUNT(*) AS orphan_tournament_players
FROM resenha.tournament_player tp
LEFT JOIN resenha.tournament t ON t.id = tp.tournament_id
LEFT JOIN resenha.player p ON p.id = tp.player_id
WHERE t.id IS NULL OR p.id IS NULL;
```

## 5. Sequence sanity

```sql
SELECT 'users_id_seq' AS seq, last_value, (SELECT max(id) FROM resenha.users) AS max_id FROM resenha.users_id_seq
UNION ALL SELECT 'player_id_seq', last_value, (SELECT max(id) FROM resenha.player) FROM resenha.player_id_seq
UNION ALL SELECT 'wallet_id_seq', last_value, (SELECT max(id) FROM resenha.wallet) FROM resenha.wallet_id_seq
UNION ALL SELECT 'tournament_id_seq', last_value, (SELECT max(id) FROM resenha.tournament) FROM resenha.tournament_id_seq
UNION ALL SELECT 'tournament_round_id_seq', last_value, (SELECT max(id) FROM resenha.tournament_round) FROM resenha.tournament_round_id_seq
UNION ALL SELECT 'event_id_seq', last_value, (SELECT max(id) FROM resenha.event) FROM resenha.event_id_seq
UNION ALL SELECT 'market_id_seq', last_value, (SELECT max(id) FROM resenha.market) FROM resenha.market_id_seq
UNION ALL SELECT 'outcome_id_seq', last_value, (SELECT max(id) FROM resenha.outcome) FROM resenha.outcome_id_seq
UNION ALL SELECT 'bet_slip_id_seq', last_value, (SELECT max(id) FROM resenha.bet_slip) FROM resenha.bet_slip_id_seq
UNION ALL SELECT 'bet_slip_item_id_seq', last_value, (SELECT max(id) FROM resenha.bet_slip_item) FROM resenha.bet_slip_item_id_seq
UNION ALL SELECT 'transaction_id_seq', last_value, (SELECT max(id) FROM resenha.transaction) FROM resenha.transaction_id_seq;
```

## 6. Market/outcome consistency

```sql
SELECT m.id, m.event_id, m.market_type, COUNT(o.id) AS outcomes
FROM resenha.market m
JOIN resenha.outcome o ON o.market_id = m.id
GROUP BY m.id, m.event_id, m.market_type
ORDER BY m.id;

## 7. Penalty shootout check

```sql
SELECT id, home_score, away_score, penalties_home, penalties_away, status
FROM resenha.event
WHERE is_knockout = TRUE
  AND home_score = away_score
  AND penalties_home IS NOT NULL;
```
```
