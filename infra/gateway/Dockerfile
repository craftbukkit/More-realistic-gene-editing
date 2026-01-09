# Dockerfile for Nginx Gateway with OpenSSL 3.5

FROM ubuntu:22.04

# Install build dependencies
RUN apt-get update && apt-get install -y wget build-essential

# Download and build OpenSSL 3.5
RUN wget https://www.openssl.org/source/openssl-3.5.0.tar.gz && \
    tar -xvf openssl-3.5.0.tar.gz && \
    cd openssl-3.5.0 && \
    ./config && \
    make && \
    make install

# Download and build Nginx
RUN wget http://nginx.org/download/nginx-1.25.3.tar.gz && \
    tar -xvf nginx-1.25.3.tar.gz && \
    cd nginx-1.25.3 && \
    ./configure --with-http_ssl_module --with-openssl=../openssl-3.5.0 && \
    make && \
    make install

COPY nginx.conf /usr/local/nginx/conf/nginx.conf

EXPOSE 80 443

CMD ["/usr/local/nginx/sbin/nginx", "-g", "daemon off;"]
