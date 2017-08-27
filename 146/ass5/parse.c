#include "parse.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <unistd.h>
#include <stdbool.h>
#include <sys/types.h>
#include <sys/wait.h>

#define LIMIT 10240

int main(){
    char input[LIMIT];
    int count = 0;
    char *next = NULL;
    cmd_t cmd;
    char *in = input;
    // cmd.output = malloc(sizeof(char) * 100);
    // in = "/";
    // if(parse(in,&cmd,100) == 0){
    //     printf("%d:%s",cmd.cmd_nr,cmd.output);
    // }
    // in = "d > e > f";
    // if(parse(in,&cmd,100) == 0){
    //     printf("%d:%s",cmd.cmd_nr,cmd.output);
    //     free(cmd.output);
    // }
    // return 0;
    while(1){
        printf("?> ");
        if(!fgets(in,LIMIT,stdin)){
            perror("Cant read input");
            exit(0);
        }
            
        
        int size = strlen(in) * 3 +1;
        //create a buffer that is three times the size of the original input, which should be big enough to hold the result
        
        cmd.output = malloc(sizeof(char) * size);
        if(parse(in,&cmd,size) != 0){
            perror("Parsing Failed");
            exit(0);
        }
            
        
        printf("%d:%s",cmd.cmd_nr,cmd.output);
        free(cmd.output);
    }
    
}


//put single quote around the next word in the given string pointer to pointer pinput, and write in the dereference of pout 
//side effect: pinput point to the next non-char or non-integer character in the given string, or 0 if end of string is encountered
//              pout value point to the next available buffer space

void quote_input(char **pinput,char **pout){
    char *input = *pinput;
    char *out = *pout, *bak = *pout;
    *out++ = '\'';
    while(isalnum(*input)){
        *out++ = *input++;
    }
        
    *out = '\'';

    printf("quote %s\n",bak);
    *pinput = input;
    *pout = bak;
}

//skip any white space in the given pointer to string
//side effect: *pinput point to the next non-whitespace char, or 0 if end of string is encountered
void skip_space(char **pinput){
    char* input = *pinput;
    while(*input && *input == ' ')
        input++;
    *pinput = input;
}

//skip any duplicate space in the given pointer to string
//side effect: if *pinput point to a series of whitespaces, *pinput will point at last whitespace, or 0 if end of string
//              if *pinput point to a non-whitespace, nothing gets changed
void skip_dup_space(char **pinput){
    if(**pinput == ' '){
        skip_space(pinput);
        *pinput -= 1;
    }
    
}
//parse the given command string
//in : command input from user
//cmd : struct for holding parsed output
//buffer_limit: the size of the buffer in cmd struct
//return : 0 if parsing is successful, 1 if not
int parse(char *in, cmd_t* cmd, int buffer_limit){
    int count = 0;
    char *out_start = cmd->output;
    char *out = out_start;
    char *input = in;
    int limit = 128;
    char *redirect_in_start = calloc( limit, sizeof(char) );
    char *redirect_in = redirect_in_start;
    char *redirect_out_start = calloc( limit, sizeof(char) );
    char *redirect_out = redirect_out_start;
    int count_redirect_in = 0;
    while(*input){
        if(isalnum(*input)){
            quote_input(&input,&out);
            // printf("quote %s\n",out_start);
        }else if(*input == '|'){
            count++;
        }else if(*input == '<'){
            //buffer the redirect into a separate char pointer, and append it to the start of the output later on
            char *bak = redirect_in;
            *redirect_in++ = *input++;
            skip_space(&input);
            
            quote_input(&input,&redirect_in);
            *redirect_in++ = ' ';
            //count the number of characters for redirect string
            count_redirect_in = redirect_in - bak;
            redirect_in = bak;

            // printf("curr %c \n",*input);
            skip_space(&input);
            continue;
        }else if(*input == '>'){
            
            char *bak = redirect_out;
            *redirect_out++ = *input++;
            skip_space(&input);
            // printf("curr in %s out %s \n",input,redirect_out);
            quote_input(&input,&redirect_out);
            // printf("curr in %s out %s \n",input,redirect_out);
            redirect_out = bak;

            // printf("curr %s \n",redirect_out);
            skip_space(&input);
            continue;
        }
        if((out - out_start) > buffer_limit)
            return 1;
        skip_dup_space(&input);

        *out++ = *input++;
    }


    int jmp_size = count_redirect_in;

    //if the buffer big enough to continue?
    if((out - out_start) + jmp_size +1> buffer_limit)
            return 1;
    //shift the original string to the right by the size of redirect string
    for(int i=(out - out_start);i>=0;i--){
        out_start[i + jmp_size] = out_start[i];
    }

    
    //append redirect string to the start
    while(*redirect_in){
        *out_start++ = *redirect_in++;
    }
    //move the cursor to the end of the string
    out += jmp_size > 0 ? jmp_size - 1 : 0;

    while(*redirect_out){
        *out++ = *redirect_out++;
    }

    *out++ = '\n';
    *out = '\0';

    cmd->cmd_nr = count +1;
    free(redirect_in_start);
    free(redirect_out_start);
    return 0;
}
