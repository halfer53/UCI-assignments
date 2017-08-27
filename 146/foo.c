#include <stdio.h>
int main ( int argc, char **argv ) {
	int i=0;
	printf("hello world\n");
	for(;i<argc;i++){
		printf("%s\n", argv[i]);
	}
	return 0;
}
