name := "sns"

version := "0.4.2"

scalaVersion := "2.13.18"

scalacOptions ++= Seq("-deprecation", "-feature")

// sbt-assembly
assembly / assemblyJarName  := s"sns-${version.value}.jar"
assembly / test := {}

assembly / assemblyMergeStrategy  := {
 case PathList("META-INF", _*) => MergeStrategy.discard
 case "reference.conf"         => MergeStrategy.concat
 case "application.conf"       => MergeStrategy.concat
 case _                        => MergeStrategy.first
}

val pekkoVersion = "1.6.0"
val pekkoHttpVersion = "1.3.0"
val camelVersion = "3.14.8"

libraryDependencies ++= {
  //noinspection SbtDependencyVersionInspection
  Seq(
    "org.apache.pekko" %% "pekko-actor" % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
    "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-http-xml" % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
    "ch.qos.logback" % "logback-classic" % "1.3.15",
    "net.logstash.logback"    %  "logstash-logback-encoder"  % "7.4",
    "com.amazonaws" % "aws-java-sdk-sqs" % "1.12.797",
    ("org.apache.camel" % "camel-aws" % "2.25.4").excludeAll(ExclusionRule(organization = "com.amazonaws")),
    "org.apache.camel" % "camel-http" % camelVersion,
    "org.apache.camel" % "camel-rabbitmq" % "3.22.4",
    "org.apache.camel" % "camel-slack" % camelVersion exclude("junit", "junit"),
    "org.scalatest" %% "scalatest" % "3.2.20" % Test,
    "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpVersion % Test,
    "org.apache.pekko" %% "pekko-testkit" % pekkoVersion % Test
    )
}

dependencyOverrides += "org.apache.pekko" %% "pekko-actor" % pekkoVersion
dependencyOverrides += "org.apache.pekko" %% "pekko-stream" % pekkoVersion
