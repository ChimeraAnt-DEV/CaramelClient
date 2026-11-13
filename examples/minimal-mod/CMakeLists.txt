cmake_minimum_required(VERSION 3.22)

project(caramel_minimal_mod LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# The example builds against the sweetloader's public headers only. Point
# SWEETLOADER_ANDROID_ROOT at a preloader-android checkout (the client's submodule is one).
get_filename_component(CARAMEL_REPO_ROOT "${CMAKE_CURRENT_LIST_DIR}/../.." ABSOLUTE)
set(SWEETLOADER_ANDROID_ROOT
    "${CARAMEL_REPO_ROOT}/app/src/main/cpp/sweetloader"
    CACHE PATH "Path to a preloader-android checkout")

if(NOT EXISTS "${SWEETLOADER_ANDROID_ROOT}/include/pl/Mod.hpp")
    message(FATAL_ERROR
        "SWEETLOADER_ANDROID_ROOT must point to a preloader-android checkout. "
        "Missing: ${SWEETLOADER_ANDROID_ROOT}/include/pl/Mod.hpp")
endif()

add_library(minimal_mod SHARED src/MinimalMod.cpp)
target_include_directories(minimal_mod PRIVATE
    "${SWEETLOADER_ANDROID_ROOT}/include")
target_compile_options(minimal_mod PRIVATE
    $<$<COMPILE_LANG_AND_ID:CXX,Clang,GNU>:-Wall -Wextra -Wpedantic>)
set_target_properties(minimal_mod PROPERTIES
    OUTPUT_NAME "minimal_mod"
    CXX_VISIBILITY_PRESET hidden
    VISIBILITY_INLINES_HIDDEN ON)

# The mod resolves the sweetloader's exports from the host process at load time, so it is linked
# with unresolved symbols allowed rather than against a sweetloader .so (which is not shipped as a
# linkable artifact).
if(ANDROID)
    target_link_options(minimal_mod PRIVATE -Wl,--unresolved-symbols=ignore-all)
    find_library(CARAMEL_LOG_LIB log)
    if(CARAMEL_LOG_LIB)
        target_link_libraries(minimal_mod PRIVATE "${CARAMEL_LOG_LIB}")
    endif()
endif()
