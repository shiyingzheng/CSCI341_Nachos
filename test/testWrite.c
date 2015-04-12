#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int main(){
    int i;
    int j;
    char x[1000];
    for(i=0;i<20;i++){
	for(j=0;j<i+1;j++){
	    x[j]='x';
	}
	x[j] = 0;
	int f = creat(x);
	if(f == -1){
	    printf("Unable to create file\n");
	}
	else{
	    write(f,"Wrote to the file!\n",20);
	}
    }
}
