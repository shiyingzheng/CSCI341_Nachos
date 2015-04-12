#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main(){
    int i;
    int j;
    for(i=0;i<10000;i++){
	char x[255];
	for(j=0;j<i-1;j++){
	    x[j]='x';
	}
	x[i] = 0;
	int f = creat(x);
    }
}
