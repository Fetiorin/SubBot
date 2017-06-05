FROM hseeberger/scala-sbt

COPY . /app
WORKDIR /app

EXPOSE 8071
EXPOSE 9000
EXPOSE 27017

CMD ["sbt", "run"]
