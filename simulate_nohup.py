#!/usr/bin/env python3
"""
simulate_nohup.py
 
Simulates the dump_udp_ow_17 LOFAR data capture pipeline by appending
realistic log lines to nohup.out every 2-5 minutes.
 
Usage:
    python simulate_nohup.py                   # appends to nohup.out in CWD
    python simulate_nohup.py /path/nohup.out   # custom path
 
Stop with Ctrl+C.
"""
 
import sys
import os
import time
import random
import math
from datetime import datetime, timedelta
 
# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
NOHUP_PATH = sys.argv[1] if len(sys.argv) > 1 else "nohup.out"
 
PORT = 16140
PACK_LEN = 7824
OWN_BUFFER_SIZE = 1_000_000_000
MAX_BUFF_SIZE   = 1_000_001_536
FILENAME_BASE   = "starlink"
 
# How many ~0.93 GB blocks to write per "session" before a SKIP/timeout
BLOCKS_PER_SESSION_RANGE = (40, 75)
 
# Interval between appended blocks (seconds) — 2-5 minutes
INTERVAL_MIN = 120
INTERVAL_MAX = 300
 
 
# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
 
def ts() -> str:
    """Current timestamp for logging."""
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
 
 
def fmt_datetime(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%dT%H:%M:%S.000")
 
 
def rand_buff() -> int:
    """Realistic max-buff values seen in the real file."""
    choices = [312960, 320784, 336432, 399024, 414672, 547680, 2_151_600]
    return random.choice(choices)
 
 
def buff_pct(buff: int) -> str:
    pct = buff / MAX_BUFF_SIZE * 100
    if pct < 0.1:
        return "0.0"
    return f"{pct:.1f}"
 
 
def mean_frac(total_gb: float) -> str:
    """Decaying mean fraction, similar to what the real tool prints."""
    val = 1e-7 / (total_gb + 0.1)
    return f"{val:.3e}"
 
 
def missed_pct(total_packets: int, block_num: int) -> float:
    """
    Real file shows ~18-40% missed for blocks with 213261 expected,
    and 0% for blocks with 127812 expected.
    We alternate between the two block sizes.
    """
    if block_num % 2 == 0:
        return round(random.uniform(18.0, 42.0), 6)
    return 0.0
 
 
def exp_packets(block_num: int) -> int:
    """Alternates between two realistic expected-packet counts."""
    return 213_261 if block_num % 2 == 0 else 127_812
 
 
def cumulative_exp(block_num: int) -> int:
    """Running total of expected packets after `block_num` blocks."""
    full_pairs = block_num // 2
    remainder  = block_num % 2
    total = full_pairs * (213_261 + 127_812)
    if remainder:
        total += 213_261
    return total
 
 
def cum_missed_pct(cum_exp: int, cum_missed: int) -> float:
    if cum_exp == 0:
        return 0.0
    return round(cum_missed / cum_exp * 100, 6)
 
 
def compression_ratio() -> float:
    return round(random.uniform(34.0, 37.5), 3)
 
 
# ---------------------------------------------------------------------------
# Block / session generators
# ---------------------------------------------------------------------------
 
def make_block_line(
    cum_gb: float,
    buff: int,
    mean: str,
    cum_exp: int,
    cum_missed_p: float,
    blk_exp: int,
    blk_missed_p: float,
) -> str:
    """Produces a pair of stat lines (cumulative + block)."""
    lines = []
    lines.append(
        f"total {cum_gb:6.3f} GB  "
        f"max buff {buff}/{MAX_BUFF_SIZE} ({buff_pct(buff)} % full)  "
        f"mean frac {mean}"
    )
    lines.append("")
    lines.append(
        f"port {PORT} : {cum_exp:7d} exp  {cum_missed_p:10.6f} % missed "
        f"   0.000000 % dropped  {cum_gb:6.3f} GB"
    )
    lines.append(
        f"                           100.000000 % good"
    )
    lines.append(
        f"      block: {blk_exp:7d} exp  {blk_missed_p:10.6f} % missed "
        f"   0.000000 % dropped"
    )
    lines.append(
        f"                           100.000000 % good"
    )
    return "\n".join(lines)
 
 
def make_session_header(start_dt: datetime, skip_init: int) -> str:
    lines = []
    lines.append(f"SKIP: init {skip_init}: 1 for nsock=1")
    lines.append("SKIP: still to skip 1 packets in socket 0")
    lines.append("start compression pipe")
    lines.append("")
    lines.append(f"creating {FILENAME_BASE}_{PORT}.radio.{fmt_datetime(start_dt)}_0000.zst")
    return "\n".join(lines)
 
 
def make_session_footer(
    start_dt: datetime,
    cum_gb: float,
    cum_exp: int,
    cum_missed: int,
    buff: int,
    mean: str,
    skip_init: int,
    reason: str = "timeout",
) -> str:
    missed_p  = cum_missed_pct(cum_exp, cum_missed)
    seen      = cum_exp - cum_missed
    seen_pct  = round(100 - missed_p, 6)
    raw_bytes = int(cum_gb * 1_073_741_824)
    comp_bytes = int(raw_bytes * (compression_ratio() / 100))
 
    lines = []
    lines.append(f"SKIP: init {skip_init}: 1 for nsock=1")
    lines.append("SKIP: still to skip 1 packets in socket 0")
    lines.append(reason)
    lines.append("MYDEBUG consumer(), line 1229  detected stopped==1  oldpoi=(nil)")
    lines.append("MYDEBUG consumer(), line 1249  my_stopped==1")
    lines.append("")
    lines.append("total per socket:  (with checks for beamformed data and with checks for packets dropped by kernel)")
    lines.append(f"port {PORT} :  expected packets {cum_exp:10d}")
    lines.append(f"                missed packets {cum_missed:10d}  {missed_p:10.6f} % of exp")
    lines.append(f"                  seen packets {seen:10d}  {seen_pct:10.6f} % of exp")
    lines.append(f"                  good packets {seen:10d}   100.000000 % of seen")
    lines.append(f"               dropped packets          0     0.000000 % of seen")
    lines.append(f"               written packets {seen:10d}   100.000000 % of seen")
    lines.append(f"                                            {seen_pct:10.6f} % of exp")
    kernel_drop = random.choice([0, 636])
    if seen > 0:
        kd_pct = round(kernel_drop / (seen + kernel_drop) * 100, 6) if kernel_drop else 0.0
    else:
        kd_pct = 0.0
    lines.append(f"         dropped by kernel  {kernel_drop:7d}  {kd_pct:10.6f} % of (seen+dropped by kernel)")
    lines.append(f"                   volume  {cum_gb:8.3f} GB")
    lines.append("")
    lines.append(f"total {cum_gb:6.3f} GB  max buff {buff}/{MAX_BUFF_SIZE} ({buff_pct(buff)} % full)  mean frac {mean}")
    lines.append(f"closing {FILENAME_BASE}_{PORT}.radio.{fmt_datetime(start_dt)}_0000.zst")
    lines.append(f"compression: {raw_bytes} -> {comp_bytes}  reduced to {compression_ratio()} %")
    lines.append("MYDEBUG consumer(), line 1342  clearing stopped flag")
    return "\n".join(lines)
 
 
# ---------------------------------------------------------------------------
# Main simulation
# ---------------------------------------------------------------------------
 
def generate_session(start_dt: datetime, skip_init: int) -> str:
    """
    Builds one complete session worth of log text (header + N blocks + footer).
    Returns the full string and the number of blocks so the caller can decide
    how many to append incrementally.
    """
    n_blocks = random.randint(*BLOCKS_PER_SESSION_RANGE)
    buff     = rand_buff()
 
    cum_gb      = 0.0
    cum_exp     = 0
    cum_missed  = 0
 
    all_blocks = []
 
    for i in range(n_blocks):
        blk_exp      = exp_packets(i)
        blk_missed_p = missed_pct(cum_exp, i)
        blk_missed   = int(blk_exp * blk_missed_p / 100)
 
        cum_gb      += 0.931
        cum_exp     += blk_exp
        cum_missed  += blk_missed
        mean        = mean_frac(cum_gb)
        cum_m_pct   = cum_missed_pct(cum_exp, cum_missed)
 
        block_text = make_block_line(
            cum_gb, buff, mean,
            cum_exp, cum_m_pct,
            blk_exp, blk_missed_p,
        )
        all_blocks.append(block_text)
 
    header = make_session_header(start_dt, skip_init)
    footer = make_session_footer(
        start_dt, cum_gb, cum_exp, cum_missed, buff, mean_frac(cum_gb), skip_init
    )
 
    return header, all_blocks, footer, cum_gb
 
 
def append_line(path: str, text: str):
    with open(path, "a", encoding="utf-8") as f:
        f.write(text + "\n")
 
 
def run():
    print(f"[{ts()}] simulate_nohup.py started — writing to '{NOHUP_PATH}'")
    print(f"[{ts()}] Appending one block every {INTERVAL_MIN//60}–{INTERVAL_MAX//60} minutes. Ctrl+C to stop.")
 
    skip_init   = 2
    session_dt  = datetime.now()
 
    while True:
        print(f"[{ts()}] Starting new simulated session …")
        header, blocks, footer, total_gb = generate_session(session_dt, skip_init)
 
        # Write the session header immediately
        append_line(NOHUP_PATH, header)
 
        # Append one block at a time with a random delay
        for idx, block in enumerate(blocks):
            append_line(NOHUP_PATH, block)
            wait = random.randint(INTERVAL_MIN, INTERVAL_MAX)
            print(f"[{ts()}] Block {idx+1}/{len(blocks)} written "
                  f"({total_gb/(len(blocks)) * (idx+1):.2f} GB simulated). "
                  f"Next in {wait}s …")
            time.sleep(wait)
 
        # Write session footer
        append_line(NOHUP_PATH, footer)
        print(f"[{ts()}] Session closed ({total_gb:.2f} GB). Starting next session immediately.")
 
        # Advance timestamp and skip counter for next session
        session_dt = datetime.now()
        skip_init  = 2  # stays 2 after init
 
 
if __name__ == "__main__":
    try:
        run()
    except KeyboardInterrupt:
        print(f"\n[{ts()}] Stopped by user.")
 