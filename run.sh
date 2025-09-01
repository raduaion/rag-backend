#!/bin/bash

mvn clean package -DskipTests && mvn spring-boot:run
