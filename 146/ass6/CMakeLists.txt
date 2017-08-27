cmake_minimum_required(VERSION 3.7)
project(ass6)

set(CMAKE_CXX_STANDARD 11)

set(SOURCE_FILES
        nsh.c
        nsh.h
        parse.c
        parse.h)

add_executable(ass6 ${SOURCE_FILES})