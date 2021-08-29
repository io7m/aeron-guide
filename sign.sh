#!/bin/sh
exec gpg -a --detach-sign -u 0xCB39C234E824F9EA "$@"
