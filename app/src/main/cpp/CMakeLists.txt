cmake_minimum_required(VERSION 3.22.1)

project("shadowvault")

add_library(
        shadowvault
        SHARED
        anti_detect.cpp)

find_library(
        log-lib
        log)

add_library(
        shadow_vm
        SHARED
        chaos_hypervisor.cpp)

target_link_libraries(
        shadowvault
        ${log-lib})

target_link_libraries(
        shadow_vm
        ${log-lib})

