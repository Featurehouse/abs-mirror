#!/usr/bin/env python3

#
# (c) teddyxlandlee 2023.
# Licensed under GNU Lesser General Public License, Version 3.
# See https://www.gnu.org/licenses/lgpl-3.0.html for more info.
#

import sys

WHITESPACES = '\x09\x0a\x0b\x0c\x0d\x1c\x1d\x1e\x1f\x20'

def main():
    if len(sys.argv) > 1:
        with open(sys.argv[1]) as f:
            # First 8192
            first = True
            peek = f.read(8192)
            proc_list = []
            has_unblank = ''
            while True:
            
                s = peek
                if first:
                    s = s.strip()
                    if s:
                        first = False
                peek = f.read(8192)
                if not s:
                    continue
                
                stripped = s.rstrip()
                if not stripped:
                    proc_list.append(s)
                else:
                    # start of string is non-blank. Clear cache.
                    for k in proc_list:
                        print(k, end='')
                    proc_list = []
                    has_unblank = ''
                    if len(stripped) != len(s):
                        # Still ends with blank
                        proc_list.append(s)
                        # stripped is not empty
                        has_unblank = stripped
                    else:
                        print(s, end='')
                
                if len(peek) < 8192:
                    # Ready EOF
                    peek_stripped = peek.rstrip()
                    if first:
                        peek_stripped = peek_stripped.strip()
                    if peek_stripped:
                        # starts with non-blank
                        for k in proc_list:
                            print(k, end='')
                        print(peek_stripped, end='')
                    # else starts with blank. If has_unblank, then #0 must be unblank while trailing is blank
                    elif has_unblank and proc_list:
                        # first must be false
                        print(has_unblank, end='')
                    break

if __name__ == '__main__':
    main()


