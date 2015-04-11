#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main(){
    int f = creat("5.txt");
    printf("%d\n",f);
}
