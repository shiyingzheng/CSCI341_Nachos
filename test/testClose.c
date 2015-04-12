#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main(){
    int i;
    int j;
    char x[1000];
    for(i=0;i<50;i++){
	for(j=0;j<i+1;j++){
	    x[j]='x';
	}
	x[j] = 0;
	int f = creat(x);
	close(f);
	close(f+5);
    }
}
