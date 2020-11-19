FROM openjdk:8u181-jdk-slim-stretch
LABEL maintainer="Sergey Dubov <sergey.dubov@jetbrains.com>"

RUN mkdir -p /usr/share/man/man1

# Install JDK
RUN apt-get update && apt-get install -y --no-install-recommends \
        maven \
        openjfx \
        apt-transport-https \
        wget \
        gpg \
    && rm -rf /var/lib/apt/lists/*

# Install zip and unzip
RUN apt-get update && apt-get install -y --no-install-recommends \
        zip \
        unzip \
    && rm -rf /var/lib/apt/lists/*
