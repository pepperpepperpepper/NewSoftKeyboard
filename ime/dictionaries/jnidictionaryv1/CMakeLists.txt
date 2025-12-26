cmake_minimum_required(VERSION 3.4.1)

add_library(newsoftkeyboard_jnidictionaryv1 SHARED
    src/main/jni/source/com_anysoftkeyboard_dictionaries_BinaryDictionary.cpp
    src/main/jni/source/dictionary.cpp)

target_include_directories(newsoftkeyboard_jnidictionaryv1 PRIVATE
    src/main/jni/include)
