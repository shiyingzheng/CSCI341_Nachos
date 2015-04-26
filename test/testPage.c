#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"
int test1(){
    int pageSize = 1024;
    int arraySize = 6*pageSize;
    char array[arraySize];
    int i;
    for(i=0;i<arraySize;i++){
	array[i]=i%256;
    }
    for(i=0;i<arraySize;i++){
	printf("%c",array[i]);
    }
}
int test2(){
    char* arg[1];
    char* argv[0];
    arg[0] = "testPage.coff";
    printf("APPLES ARE TASTY\n");
    int ret = exec(arg[0],0,argv);
    printf("%d", ret);
    /* printf("\n"); */
}
int test3(){
    int pageSize = 1024;
    int arraySize = 9*pageSize;
    char array[arraySize-pageSize];
    int i;
    for(i=0;i<arraySize;i++){
	array[i]=i%256;
	//printf("%d\n",i);
    }
    for(i=0;i<arraySize;i+= 1000){
	printf("%c",array[i]);
    }
}
int main(){
    test2();
}
