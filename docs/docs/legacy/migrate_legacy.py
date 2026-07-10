#!/usr/bin/env python3
"""
ResenhaBET legacy migration generator.

Reads the legacy backup JSON files and writes PostgreSQL INSERT statements for
the current resenha schema.
"""

from __future__ import annotations

import argparse
import json
import sys
import uuid
from collections import defaultdict
from dataclasses import dataclass
from decimal import Decimal
from pathlib import Path
from typing import Any, Iterable


GROUP_ID = 1
GROUP_NAME = "ResenhaBET"
DEFAULT_ODD = Decimal("1.05")


def warn(message: str) -> None:
    print(f"WARNING: {message}", file=sys.stderr)


def esc(value: str) -> str:
    return value.replace("'", "''")


def sql_str(value: str) -> str:
    return f"'{esc(value)}'"


def sql_bool(value: bool) -> str:
    return "TRUE" if value else "FALSE"


def sql_null(value: Any) -> str:
    return "NULL" if value is None else value


def sql_decimal(value: Any) -> str:
    if value is None:
        return "NULL"
    dec = value if isinstance(value, Decimal) else Decimal(str(value))
    return format(dec, "f")


def ts_to_sql(epoch_ms: int | float) -> str:
    return f"to_timestamp({repr(float(epoch_ms) / 1000.0)})"


def sql_uuid(seed: str) -> str:
    return sql_str(str(uuid.uuid5(uuid.NAMESPACE_URL, seed)))


def insert_statement(table: str, columns: list[str], values: list[str]) -> str:
    cols = ", ".join(columns)
    vals = ", ".join(values)
    return f"INSERT INTO resenha.{table} ({cols}) VALUES ({vals});"


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


@dataclass
class EventRecord:
    id: int
    legacy_id: str
    tournament_legacy_id: str
    player1: str
    player2: str
    score1: int
    score2: int
    is_knockout: bool
    phase: str


def build_identity_and_grouping(
    players: list[dict[str, Any]],
    recent: dict[str, Any],
) -> tuple[list[str], dict[str, int], dict[str, int], dict[str, int], dict[str, Decimal], dict[str, int]]:
    lines: list[str] = ["-- identity + group setup"]
    user_id_by_name: dict[str, int] = {}
    player_id_by_name: dict[str, int] = {}
    group_member_count = 0
    wallet_balance_by_user: dict[str, Decimal] = {}
    group_member_id_by_name: dict[str, int] = {}

    bettors = recent.get("bettors", {})
    player_names = [entry["name"] for entry in players]
    admin_name = "Francisco" if "Francisco" in player_names else None

    lines.append(insert_statement(
        "groups",
        ["id", "name", "is_active"],
        [str(GROUP_ID), sql_str(GROUP_NAME), "TRUE"],
    ))

    for idx, player in enumerate(players, start=1):
        name = player["name"]
        user_id_by_name[name] = idx
        user_type = "ADMIN" if admin_name == name else "USER"
        lines.append(insert_statement(
            "users",
            ["id", "name", "pin_hash", "salt", "user_type", "is_first_login"],
            [str(idx), sql_str(name), "NULL", "NULL", sql_str(user_type), "TRUE"],
        ))

    for idx, player in enumerate(players, start=1):
        name = player["name"]
        player_id_by_name[name] = idx
        lines.append(insert_statement(
            "wallet",
            ["id", "user_id", "balance"],
            [str(idx), str(user_id_by_name[name]), sql_decimal(bettors.get(name, {}).get("wallet", 0.0))],
        ))
        wallet_balance_by_user[name] = Decimal(str(bettors.get(name, {}).get("wallet", 0.0)))

    for idx, player in enumerate(players, start=1):
        name = player["name"]
        lines.append(insert_statement(
            "player",
            ["id", "name", "is_active", "current_elo", "user_id", "group_id"],
            [
                str(idx),
                sql_str(name),
                "TRUE",
                sql_decimal(player["elo"]),
                str(user_id_by_name[name]),
                str(GROUP_ID),
            ],
        ))

    for idx, player in enumerate(players, start=1):
        name = player["name"]
        group_member_id_by_name[name] = idx
        role = "OWNER" if name == admin_name else "MEMBER"
        lines.append(insert_statement(
            "group_member",
            ["id", "group_id", "user_id", "role", "player_claimed"],
            [str(idx), str(GROUP_ID), str(user_id_by_name[name]), sql_str(role), "FALSE"],
        ))

    return lines, user_id_by_name, player_id_by_name, group_member_id_by_name, wallet_balance_by_user, {}


def build_tournaments_rounds_and_group_tables(
    tournaments: list[dict[str, Any]],
    player_id_by_name: dict[str, int],
    user_id_by_name: dict[str, int],
    wallet_balance_by_user: dict[str, Decimal],
) -> tuple[list[str], dict[str, int], dict[tuple[str, str], int], dict[int, dict[str, Any]], dict[int, dict[str, Any]], int]:
    lines: list[str] = ["", "-- tournaments + rounds + group_tournament"]
    tournament_id_by_legacy: dict[str, int] = {}
    round_id_by_key: dict[tuple[str, str], int] = {}
    group_tournament_by_tournament_id: dict[int, dict[str, Any]] = {}
    tournament_by_id: dict[int, dict[str, Any]] = {}
    tournament_player_count = 0

    tournament_count = 0
    round_count = 0
    group_tournament_count = 0
    tournament_wallet_count = 0
    group_tournament_market_type_count = 0

    for t_idx, tourn in enumerate(tournaments, start=1):
        tournament_count += 1
        legacy_id = tourn["id"]
        tournament_id_by_legacy[legacy_id] = t_idx
        phase_names = [phase["name"] for phase in tourn.get("phases", [])]
        format_value = "LEAGUE_BRACKET" if any("Liga" in name for name in phase_names) else "BRACKET"
        has_third_place = any("Terceiro" in name for name in phase_names)
        lines.append(insert_statement(
            "tournament",
            [
                "id",
                "uuid",
                "name",
                "type",
                "format",
                "status",
                "start_date",
                "end_date",
                "created_at",
                "generation_mode",
                "has_third_place_match",
                "number_of_groups",
                "players_advancing_per_group",
                "version",
                "competition_id",
            ],
            [
                str(t_idx),
                sql_uuid(f"legacy:tournament:{legacy_id}"),
                sql_str(tourn["name"]),
                sql_str("FIFA_MATCH"),
                sql_str(format_value),
                sql_str("COMPLETED"),
                "NULL",
                "NULL",
                ts_to_sql(tourn["createdAt"]),
                sql_str("MANUAL"),
                sql_bool(has_third_place),
                "1",
                "2",
                "0",
                "NULL",
            ],
        ))
        tournament_by_id[t_idx] = tourn

        for r_idx, phase in enumerate(tourn.get("phases", []), start=1):
            round_count += 1
            round_id = round_count
            round_id_by_key[(legacy_id, phase["name"])] = round_id
            phase_type = "GROUP_STAGE" if "Liga" in phase["name"] else "KNOCKOUT"
            lines.append(insert_statement(
                "tournament_round",
                ["id", "tournament_id", "name", "multiplier", "round_order", "phase_type", "group_number"],
                [
                    str(round_id),
                    str(t_idx),
                    sql_str(phase["name"]),
                    sql_decimal(phase["multiplier"]),
                    str(r_idx - 1),
                    sql_str(phase_type),
                    "NULL",
                ],
            ))

        group_tournament_id = t_idx
        group_tournament_by_tournament_id[t_idx] = {
            "id": group_tournament_id,
            "legacy_tournament_id": legacy_id,
        }
        group_tournament_count += 1
        lines.append(insert_statement(
            "group_tournament",
            ["id", "group_id", "tournament_id"],
            [str(group_tournament_id), str(GROUP_ID), str(t_idx)],
        ))

        group_tournament_market_type_count += 1
        lines.append(insert_statement(
            "group_tournament_market_type",
            ["group_tournament_id", "market_type"],
            [str(group_tournament_id), sql_str("MATCH_RESULT")],
        ))

        for player_name, player_id in player_id_by_name.items():
            tournament_player_count += 1
            lines.append(insert_statement(
                "tournament_player",
                ["id", "tournament_id", "player_id", "team_id", "group_number"],
                [
                    str(tournament_player_count),
                    str(t_idx),
                    str(player_id),
                    "NULL",
                    "NULL",
                ],
            ))

    for user_name, user_id in user_id_by_name.items():
        for tourn_id in range(1, len(tournaments) + 1):
            tournament_wallet_count += 1
            balance = wallet_balance_by_user.get(user_name, Decimal("0"))
            lines.append(insert_statement(
                "tournament_wallet",
                ["id", "group_tournament_id", "user_id", "balance", "initial_balance"],
                [
                    str(tournament_wallet_count),
                    str(tourn_id),
                    str(user_id),
                    sql_decimal(balance),
                    sql_decimal(balance),
                ],
            ))

    return lines, tournament_id_by_legacy, round_id_by_key, group_tournament_by_tournament_id, tournament_by_id, tournament_player_count


def build_events_from_backup(
    backup: dict[str, Any],
    player_id_by_name: dict[str, int],
    tournament_id_by_legacy: dict[str, int],
    round_id_by_key: dict[tuple[str, str], int],
) -> tuple[list[str], list[EventRecord], dict[str, int]]:
    lines: list[str] = ["", "-- events from backup"]
    records: list[EventRecord] = []
    legacy_match_id_to_event_id: dict[str, int] = {}

    event_id = 0
    for match in backup.get("matches", []):
        tournament_legacy_id = match.get("tournamentId")
        if tournament_legacy_id not in tournament_id_by_legacy:
            warn(f"missing tournament for match {match.get('id')}: {tournament_legacy_id}")
            continue
        round_key = (tournament_legacy_id, match.get("phase"))
        if round_key not in round_id_by_key:
            warn(f"missing round for match {match.get('id')}: {round_key}")
            continue
        home_name = match.get("player1")
        away_name = match.get("player2")
        if home_name not in player_id_by_name or away_name not in player_id_by_name:
            warn(f"missing player for match {match.get('id')}: {home_name} vs {away_name}")
            continue

        event_id += 1
        legacy_match_id = match["id"]
        legacy_match_id_to_event_id[legacy_match_id] = event_id
        is_knockout = "Liga" not in match.get("phase", "")
        penalties_home = None
        penalties_away = None
        if match.get("onPenalties") and match.get("score1") == match.get("score2"):
            if match.get("winner") == home_name:
                penalties_home = 3
                penalties_away = 0
            elif match.get("winner") == away_name:
                penalties_home = 0
                penalties_away = 3
            else:
                warn(f"penalty match winner does not match players for {legacy_match_id}: {match.get('winner')}")
        records.append(EventRecord(
            id=event_id,
            legacy_id=legacy_match_id,
            tournament_legacy_id=tournament_legacy_id,
            player1=home_name,
            player2=away_name,
            score1=match["score1"],
            score2=match["score2"],
            is_knockout=is_knockout,
            phase=match.get("phase"),
        ))
        lines.append(insert_statement(
            "event",
            [
                "id",
                "tournament_id",
                "round_id",
                "player_home_id",
                "home_elo_before",
                "player_away_id",
                "away_elo_before",
                "game_datetime",
                "status",
                "home_score",
                "away_score",
                "is_knockout",
                "is_bye",
                "penalties_home",
                "penalties_away",
                "next_round_event_id",
                "home_source_event_id",
                "away_source_event_id",
                "is_third_place_match",
                "team_home_id",
                "team_away_id",
                "external_match_id",
            ],
            [
                str(event_id),
                str(tournament_id_by_legacy[tournament_legacy_id]),
                str(round_id_by_key[round_key]),
                str(player_id_by_name[home_name]),
                "NULL",
                str(player_id_by_name[away_name]),
                "NULL",
                ts_to_sql(match["timestamp"]),
                sql_str("COMPLETED"),
                str(match["score1"]),
                str(match["score2"]),
                sql_bool(is_knockout),
                "FALSE",
                str(penalties_home) if penalties_home is not None else "NULL",
                str(penalties_away) if penalties_away is not None else "NULL",
                "NULL",
                "NULL",
                "NULL",
                sql_bool("Terceiro" in match.get("phase", "")),
                "NULL",
                "NULL",
                "NULL",
            ],
        ))

    return lines, records, legacy_match_id_to_event_id


def build_markets_outcomes(
    events: list[EventRecord],
    bet_history_by_event_id: dict[int, dict[str, Any]],
) -> tuple[list[str], dict[tuple[int, str], int]]:
    lines: list[str] = ["", "-- markets + outcomes"]
    outcome_id_by_key: dict[tuple[int, str], int] = {}
    market_id = 0
    outcome_id = 0

    for event in events:
        market_id += 1
        lines.append(insert_statement(
            "market",
            ["id", "event_id", "name", "status", "market_type"],
            [str(market_id), str(event.id), sql_str("Resultado Final"), sql_str("CLOSED"), sql_str("MATCH_RESULT")],
        ))

        bet_history = bet_history_by_event_id.get(event.id)
        if bet_history:
            odds = bet_history["match"]["odds"]
            home_odd = Decimal(str(odds["p1"]))
            away_odd = Decimal(str(odds["p2"]))
        else:
            home_odd = DEFAULT_ODD
            away_odd = DEFAULT_ODD

        labels = [("Vitória Casa", home_odd), ("Vitória Fora", away_odd)]
        if not event.is_knockout:
            labels.insert(1, ("Empate", DEFAULT_ODD))

        for label, odd in labels:
            outcome_id += 1
            outcome_id_by_key[(event.id, label)] = outcome_id
            lines.append(insert_statement(
                "outcome",
                ["id", "market_id", "name", "odd"],
                [str(outcome_id), str(market_id), sql_str(label), sql_decimal(odd)],
            ))

    return lines, outcome_id_by_key


def build_bet_slips_items_transactions(
    recent: dict[str, Any],
    player_id_by_name: dict[str, int],
    user_id_by_name: dict[str, int],
    tournament_id_by_legacy: dict[str, int],
    legacy_match_id_to_event_id: dict[str, int],
    backup_matches_by_legacy_id: dict[str, dict[str, Any]],
    outcome_id_by_key: dict[tuple[int, str], int],
) -> tuple[list[str], dict[str, int]]:
    lines: list[str] = ["", "-- bet slips + items + transactions"]
    bet_slip_id = 0
    bet_slip_item_id = 0
    transaction_id = 0
    counts = {"bet_slip": 0, "bet_slip_item": 0, "transaction": 0}
    for bet_history in recent.get("betHistory", []):
        match = bet_history.get("match", {})
        resolved_event_id = legacy_match_id_to_event_id.get(match.get("id"))
        if resolved_event_id is None:
            warn(f"could not resolve betHistory match {match.get('id')} to a backup event")
            continue

        event = None
        for legacy_match_id, event_id in legacy_match_id_to_event_id.items():
            if event_id == resolved_event_id:
                event = backup_matches_by_legacy_id.get(legacy_match_id)
                break
        if event is None:
            continue

        if event.get("tournamentId") not in tournament_id_by_legacy:
            warn(f"betHistory match {match.get('id')} has no tournament mapping")
            continue

        home_name = match.get("p1")
        away_name = match.get("p2")
        if home_name not in user_id_by_name or away_name not in user_id_by_name:
            warn(f"missing bettor player map for betHistory match {match.get('id')}")
            continue

        odd_home = Decimal(str(match["odds"]["p1"]))
        odd_away = Decimal(str(match["odds"]["p2"]))
        bet_time = ts_to_sql(int(match["id"].split("_")[1]))
        group_tournament_id = tournament_id_by_legacy[event["tournamentId"]]

        for bet in bet_history.get("bets", []):
            bettor = bet.get("bettor")
            if bettor not in user_id_by_name:
                warn(f"missing bettor user for betHistory match {match.get('id')}: {bettor}")
                continue

            bet_slip_id += 1
            status = "WON" if bet.get("outcome") == "win" else "LOST"
            on_name = bet.get("on")
            if on_name == home_name:
                label = "Vitória Casa"
                odd = odd_home
            elif on_name == away_name:
                label = "Vitória Fora"
                odd = odd_away
            else:
                warn(f"bet direction does not match match sides for {match.get('id')}: {on_name}")
                continue

            potential_return = Decimal(str(bet["payout"])) if bet.get("outcome") == "win" else Decimal(str(bet["amount"])) * odd

            lines.append(insert_statement(
                "bet_slip",
                ["id", "user_id", "tournament_id", "group_tournament_id", "stake", "combined_odd", "potential_return", "status", "created_at"],
                [
                    str(bet_slip_id),
                    str(user_id_by_name[bettor]),
                    str(group_tournament_id),
                    str(group_tournament_id),
                    sql_decimal(bet["amount"]),
                    sql_decimal(odd),
                    sql_decimal(potential_return),
                    sql_str(status),
                    bet_time,
                ],
            ))
            counts["bet_slip"] += 1

            bet_slip_item_id += 1
            lines.append(insert_statement(
                "bet_slip_item",
                ["id", "bet_slip_id", "event_id", "outcome_id", "odd_snapshot", "status"],
                [
                    str(bet_slip_item_id),
                    str(bet_slip_id),
                    str(resolved_event_id),
                    str(outcome_id_by_key[(resolved_event_id, label)]),
                    sql_decimal(odd),
                    sql_str(status),
                ],
            ))
            counts["bet_slip_item"] += 1

            transaction_id += 1
            lines.append(insert_statement(
                "transaction",
                ["id", "wallet_id", "tournament_wallet_id", "bet_slip_id", "type", "value", "created_at"],
                [
                    str(transaction_id),
                    "NULL",
                    str(group_tournament_id),
                    str(bet_slip_id),
                    sql_str("BET_PLACED"),
                    sql_decimal(bet["amount"]),
                    bet_time,
                ],
            ))
            counts["transaction"] += 1

            if bet.get("outcome") == "win" and Decimal(str(bet.get("payout", 0))) > 0:
                transaction_id += 1
                lines.append(insert_statement(
                    "transaction",
                    ["id", "wallet_id", "tournament_wallet_id", "bet_slip_id", "type", "value", "created_at"],
                    [
                        str(transaction_id),
                        "NULL",
                        str(group_tournament_id),
                        str(bet_slip_id),
                        sql_str("BET_WON"),
                        sql_decimal(bet["payout"]),
                        bet_time,
                    ],
                ))
                counts["transaction"] += 1

    return lines, counts


def build_sequence_resets(counts: dict[str, int]) -> list[str]:
    lines = ["", "-- sequence resets"]
    for table, count in counts.items():
        seq = f"resenha.{table}_id_seq"
        if count > 0:
            lines.append(f"SELECT setval('{seq}', {count}, true);")
        else:
            lines.append(f"SELECT setval('{seq}', 1, false);")
    return lines


def render_header(counts: dict[str, int]) -> list[str]:
    lines = ["/*", "Legacy migration row counts:"]
    for table, count in counts.items():
        lines.append(f"  - {table}: {count}")
    lines.append("*/")
    return lines


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate legacy migration SQL for ResenhaBET.")
    parser.add_argument("--backup", required=True, help="Path to resenha_bet_backup.json")
    parser.add_argument("--recent", required=True, help="Path to resenhabet_data_MAIS_RECENTE.json")
    parser.add_argument("--output", help="Optional output SQL file")
    args = parser.parse_args()

    backup = load_json(Path(args.backup))
    recent = load_json(Path(args.recent))

    players = backup.get("players", [])
    tournaments = backup.get("tournaments", [])
    backup_matches_by_id = {match["id"]: match for match in backup.get("matches", [])}

    identity_lines, user_id_by_name, player_id_by_name, _, wallet_balance_by_user, _ = build_identity_and_grouping(players, recent)
    tournaments_lines, tournament_id_by_legacy, round_id_by_key, _, _, _ = build_tournaments_rounds_and_group_tables(
        tournaments,
        player_id_by_name,
        user_id_by_name,
        wallet_balance_by_user,
    )
    event_lines, backup_events, legacy_match_id_to_event_id = build_events_from_backup(
        backup,
        player_id_by_name,
        tournament_id_by_legacy,
        round_id_by_key,
    )

    bet_history_by_event_id: dict[int, dict[str, Any]] = {}
    for bet_history in recent.get("betHistory", []):
        match = bet_history.get("match", {})
        resolved_event_id = legacy_match_id_to_event_id.get(match.get("id"))
        if resolved_event_id is None:
            warn(f"could not resolve betHistory match {match.get('id')} to a backup event")
            continue
        existing = bet_history_by_event_id.get(resolved_event_id)
        if existing is None:
            bet_history_by_event_id[resolved_event_id] = bet_history

    market_lines, outcome_id_by_key = build_markets_outcomes(backup_events, bet_history_by_event_id)
    bet_lines, bet_counts = build_bet_slips_items_transactions(
        recent,
        player_id_by_name,
        user_id_by_name,
        tournament_id_by_legacy,
        legacy_match_id_to_event_id,
        backup_matches_by_id,
        outcome_id_by_key,
    )

    counts = {
        "groups": 1,
        "users": len(players),
        "wallet": len(players),
        "player": len(players),
        "group_member": len(players),
        "tournament": len(tournaments),
        "tournament_round": sum(len(t.get("phases", [])) for t in tournaments),
        "group_tournament": len(tournaments),
        "group_tournament_market_type": len(tournaments),
        "tournament_player": len(players) * len(tournaments),
        "tournament_wallet": len(players) * len(tournaments),
        "event": len(backup_events),
        "market": len(backup_events),
        "outcome": len(outcome_id_by_key),
        **bet_counts,
    }

    sequence_counts = {
        "groups": 1,
        "users": len(players),
        "wallet": len(players),
        "player": len(players),
        "group_member": len(players),
        "tournament": len(tournaments),
        "tournament_round": sum(len(t.get("phases", [])) for t in tournaments),
        "group_tournament": len(tournaments),
        "tournament_player": len(players) * len(tournaments),
        "tournament_wallet": len(players) * len(tournaments),
        "event": len(backup_events),
        "market": len(backup_events),
        "outcome": len(outcome_id_by_key),
        "bet_slip": bet_counts["bet_slip"],
        "bet_slip_item": bet_counts["bet_slip_item"],
        "transaction": bet_counts["transaction"],
    }

    lines: list[str] = []
    lines.extend(render_header(counts))
    lines.extend(["BEGIN;", "", "SET search_path TO resenha, public;"])
    lines.extend(identity_lines)
    lines.extend(tournaments_lines)
    lines.extend(event_lines)
    lines.extend(market_lines)
    lines.extend(bet_lines)
    lines.extend(build_sequence_resets(sequence_counts))
    lines.extend(["", "COMMIT;"])

    sql = "\n".join(lines) + "\n"
    if args.output:
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(sql, encoding="utf-8")
    sys.stdout.write(sql)

    print("Migration summary:", file=sys.stderr)
    for table, count in counts.items():
        print(f"  {table}: {count}", file=sys.stderr)


if __name__ == "__main__":
    main()
