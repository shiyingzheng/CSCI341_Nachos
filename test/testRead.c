#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
#define EOF -1
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
	int f = open(x);
	if(f == -1){
	    printf("Unable to open file\n");
	}
	else{
	    read(f,x,20);
	    printf("%s\n",x);
	}
    }
}
int test2(){
    char x[1000];
    int f = open("meow");
    read(f,x,20);
    /* int g = creat("y"); */
    write(1,x,20);
    read(f,x,20);
    write(1,x,20);
}
int test3(){
    int c;
    while((c = getchar()) != EOF){
	printf("You typed: %c\n",c);
    }
}
int test4(){
    char buf[5];
    int i;
    for(i = 0;true;i++){
	int f = open("a");
	read(f,buf,i);
	buf[i] = 0;
	printf("%s\n",buf);
	close(f);
    }
}
int main(){
    test2();
    exit(0);
}
