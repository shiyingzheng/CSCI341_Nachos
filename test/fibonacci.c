#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main() {
	int i;
	int j = 100;
	for (i = 0; i < j; i++) {
		int x = fibonacci(i);
		printf("%d\n",x);
	}
	return 0;
}

int fibonacci(int x) {
	if (x < 2) {
		return x;
	} else {
		return fibonacci(x-1) + fibonacci(x-2);
	}
}
