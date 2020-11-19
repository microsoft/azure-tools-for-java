FROM openjdk:11.0.3-jdk-slim-stretch
LABEL maintainer="Sergey Dubov <sergey.dubov@jetbrains.com>"

ENV LOCAL_ENV_RUN=true

RUN mkdir -p /usr/share/man/man1

# Install packages
RUN apt-get update && apt-get install -y --no-install-recommends \
        maven \
        openjfx \
        apt-transport-https \
        wget \
        gpg \
        dirmngr \
        curl \
    && rm -rf /var/lib/apt/lists/*

# Install dotnet-sdk-3.1
RUN wget -O - https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor > microsoft.asc.gpg \
    && mv microsoft.asc.gpg /etc/apt/trusted.gpg.d/ \
    && wget https://packages.microsoft.com/config/debian/9/prod.list \
    && mv prod.list /etc/apt/sources.list.d/microsoft-prod.list \
    && chown root:root /etc/apt/trusted.gpg.d/microsoft.asc.gpg \
    && chown root:root /etc/apt/sources.list.d/microsoft-prod.list
    
RUN apt-get update \
    && apt-get update \
    && apt-get install -y dotnet-sdk-3.1

# Install Mono
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF \
&& echo "deb https://download.mono-project.com/repo/debian stable-stretch main" | tee /etc/apt/sources.list.d/mono-official-stable.list \
&& apt-get update \
&& DEBIAN_FRONTEND=noninteractive apt-get -y --no-install-recommends install \
    mono-complete \
    referenceassemblies-pcl \
    msbuild \
&& apt-get clean all
