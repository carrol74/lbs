#include <stdio.h>
#include <unistd.h>
#include "shellcode.h"
int main() 
{
 write(1, shellcode, sizeof(shellcode)-1);
 return 0;
}