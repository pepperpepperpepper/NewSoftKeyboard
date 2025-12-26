cmake_minimum_required(VERSION 3.4.1)

add_library(newsoftkeyboard_jnidictionaryv2 SHARED
    src/main/jni/source/char_utils.cpp
    src/main/jni/source/com_anysoftkeyboard_dictionaries_ResourceBinaryDictionary.cpp
    src/main/jni/source/dictionary.cpp)

target_include_directories(newsoftkeyboard_jnidictionaryv2 PRIVATE
    src/main/jni/include)

target_link_libraries(newsoftkeyboard_jnidictionaryv2
    log)
