FROM ubuntu:22.04

RUN apt update && apt install -y \
    openjdk-17-jdk \
    gcc g++ \
    python3 \
    maven

WORKDIR /app
COPY . .
RUN mvn clean package -Dmaven.test.skip=true


EXPOSE 8080
CMD ["sh","-c","java -Xmx256m -jar target/*.jar"]
