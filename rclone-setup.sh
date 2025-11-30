#!/bin/bash


RCLONE_PATH="/usr/bin/rclone"
MANPAGE_PATH="/usr/local/share/man/man1"
# check if paths already exist (if they do and not working maybe sudo mandb 
if [[ -e "$RCLONE_PATH" && -e "$MANPAGE_PATH"  ]]; then
	echo "rclone and manpage paths already exists"
	echo "make sure you ran: sudo mandb"
	echo "Exiting script...."
	exit 1
fi

curl -O https://downloads.rclone.org/rclone-current-linux-amd64.zip
unzip rclone-current-linux-amd64.zip
cd rclone-*-linux-amd64

sudo cp rclone /usr/bin/
sudo chown root:root /usr/bin/rclone
sudo chmod 755 /usr/bin/rclone

sudo mkdir -p /usr/local/share/man/man1
sudo cp rclone.1 /usr/local/share/man/man1
sudo mandb

rclone config



