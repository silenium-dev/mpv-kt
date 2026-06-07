project(example LANGUAGES CXX)

add_library(example SHARED library.cpp)
target_link_libraries(example PUBLIC EGL)
