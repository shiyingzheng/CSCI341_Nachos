#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main(){
    int i;
    int j;
    char x[1000];
    for(i=0;i<10;i++){
	for(j=0;j<i+1;j++){
	    x[j]='x';
	}
	x[j] = 0;
	int f = creat(x);
	int result = unlink(x);
    }
    for(i=0;i<10;i++){
	for(j=0;j<i+1;j++){
	    x[j]='y';
	}
	x[j] = 0;
	int f = creat(x);
    }
    for(i=0;i<10;i++){
	for(j=0;j<i+1;j++){
	    x[j]='y';
	}
	x[j] = 0;
	int f = creat(x);
	close(f);
	int result = unlink(x);
    }
}
