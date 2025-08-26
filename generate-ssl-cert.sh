#!/bin/bash

echo "Generating SSL certificate for AI Reader..."

cd "$(dirname "$0")"

if [ ! -d "reader/src/main/resources" ]; then
    echo "Creating resources directory..."
    mkdir -p "reader/src/main/resources"
fi

echo "Generating self-signed certificate..."
keytool -genkeypair -alias yuesf.cn -keyalg RSA -keysize 2048 -storetype JKS -keystore "reader/src/main/resources/yuesf.cn.jks" -validity 3650 -storepass changeit -keypass changeit -dname "CN=yuesf.cn, OU=Local Development, O=AI Reader, L=City, ST=State, C=CN" -ext "SAN=dns:localhost,ip:127.0.0.1"

if [ $? -eq 0 ]; then
    echo ""
    echo "Certificate generated successfully!"
    echo "Certificate file: reader/src/main/resources/yuesf.cn.jks"
    echo ""
    echo "Configuration in application.yml:"
    echo "  key-store-password: changeit"
    echo "  key-alias: yuesf.cn"
    echo ""
    echo "For browser access, you may need to allow invalid certificates for localhost."
else
    echo ""
    echo "Failed to generate certificate. Please check if keytool is available."
    echo "You may need to install Java JDK to get keytool."
fi