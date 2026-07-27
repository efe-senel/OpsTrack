#!/bin/sh
set -eu

case "${1:-}" in
  cpu-util)
    first=$(awk '/^cpu / { idle=$5+$6; total=0; for (i=2; i<=NF; i++) total+=$i; print idle, total; exit }' /hostfs/proc/stat)
    sleep 1
    second=$(awk '/^cpu / { idle=$5+$6; total=0; for (i=2; i<=NF; i++) total+=$i; print idle, total; exit }' /hostfs/proc/stat)
    awk -v first="$first" -v second="$second" 'BEGIN {
      split(first, a, " "); split(second, b, " ");
      total=b[2]-a[2]; idle=b[1]-a[1];
      if (total <= 0) print 0; else printf "%.2f\n", (total-idle)*100/total
    }'
    ;;
  memory-total)
    awk '/^MemTotal:/ { print $2 * 1024; exit }' /hostfs/proc/meminfo
    ;;
  memory-available)
    awk '/^MemAvailable:/ { print $2 * 1024; exit }' /hostfs/proc/meminfo
    ;;
  disk-total)
    df -P -B 1 /hostfs | awk 'NR == 2 { print $2 }'
    ;;
  disk-available)
    df -P -B 1 /hostfs | awk 'NR == 2 { print $4 }'
    ;;
  network-rx)
    awk -F '[: ]+' 'NR > 2 && $2 != "lo" { total += $3 } END { print total + 0 }' /hostfs/proc/net/dev
    ;;
  network-tx)
    awk -F '[: ]+' 'NR > 2 && $2 != "lo" { total += $11 } END { print total + 0 }' /hostfs/proc/net/dev
    ;;
  *)
    echo "Unknown metric" >&2
    exit 1
    ;;
esac
