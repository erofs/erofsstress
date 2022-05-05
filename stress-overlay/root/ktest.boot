#!/bin/bash -e

set +x

printf "Stressing EROFS in QEMU..."

mount -t tmpfs tmpfs /mnt
mkdir -p /mnt/{test,golden,log}
mount /dev/sda /mnt/log
mount -t erofs -oro /dev/sdb /mnt/test
mount -t erofs -oro /dev/sdc /mnt/golden
echo 4 > /proc/sys/vm/drop_caches
TIMEOUT=3600
WORKERS=7
SEED=123
for x in `cat /proc/cmdline`; do
	case $x in
		qemuktest.timeout=*)
			TIMEOUT="${x//qemuktest.timeout=}"
		;;
		qemuktest.seed=*)
			SEED="${x//qemuktest.seed=}"
		;;
	esac
done

echo 100000000 > /proc/sys/fs/nr_open
ulimit -n 100000000

set -e
timeout -k30 $TIMEOUT stdbuf -o0 -e0 /root/stress -p$WORKERS -s$SEED -l0 -d/mnt/log/baddump /mnt/test /mnt/golden || \
	[ $? -ne 124 ] && { sync; exit; }
echo 0 > /mnt/log/exitstatus
sync
umount /mnt/log
