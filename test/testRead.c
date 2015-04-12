#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int test1(){
    int i;
    int j;
    char x[1000];
    char y[1000];
    for(i=0;i<20;i++){
	for(j=0;j<i+1;j++){
	    x[j]='x';
	    y[j]='y';
	}
	x[j] = 0;
	y[j] = 0;
	int f = creat(x);
	if(f == -1){
	    printf("Unable to create file\n");
	}
	else{
	    write(f,"Four",4);
	}
	close(f);
    }
    for(i=0;i<20;i++){
	for(j=0;j<i+1;j++){
	    x[j]='x';
	    y[j]='y';
	}
	x[j] = 0;
	y[j] = 0;
	int f = creat(x);
	int g = creat(y);
	if(f == -1){
	    printf("Unable to create file\n");
	}
	else{
	    read(f,x,20);
	    write(g,x,20);
	}
    }
}
int test2(){
    char x[1000];
    int f = creat("testCreate.c");
    read(f,x,20);
    int g = creat("y");
    write(g,x,20);
}
int main(){
    test2();
}
