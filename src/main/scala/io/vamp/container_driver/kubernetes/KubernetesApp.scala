package io.vamp.container_driver.kubernetes

import io.vamp.container_driver.Docker
import org.json4s._
import org.json4s.native.Serialization._

case class KubernetesApp(
    name:       String,
    docker:     Docker,
    replicas:   Int,
    cpu:        Double,
    mem:        Int,
    privileged: Boolean,
    env:        Map[String, String],
    cmd:        List[String],
    args:       List[String],
    labels:     Map[String, String],
    dialect:    Map[String, Any]    = Map()
) extends KubernetesArtifact {

  override def toString: String = {
    val base =
      s"""
         |{
         |  "apiVersion": "apps/v1beta1",
         |  "kind": "Deployment",
         |  "metadata": {
         |    "name": "$name"
         |  },
         |  "spec": {
         |    "replicas": $replicas,
         |    "template": {
         |      "metadata": {
         |        ${labels2json(labels)}
         |      },
         |      "spec": {
         |        "containers": [{
         |          "image": "${docker.image}",
         |          "name": "$name",
         |          "env": [${env.map({ case (n, v) ⇒ s"""{"name": "$n", "value": "$v"}""" }).mkString(", ")}],
         |          "ports": [${docker.portMappings.map(pm ⇒ s"""{"name": "p${pm.containerPort}", "containerPort": ${pm.containerPort}, "protocol": "${pm.protocol.toUpperCase}"}""").mkString(", ")}],
         |          "args": [${args.map(str ⇒ s""""$str"""").mkString(", ")}],
         |          "command": [${cmd.map(str ⇒ s""""$str"""").mkString(", ")}],
         |          "resources": {
         |            "requests": {
         |              "cpu": $cpu,
         |              "memory": "${mem}Mi"
         |            }
         |          },
         |          "securityContext": {
         |            "privileged": $privileged
         |          }
         |        }]
         |      }
         |    }
         |  }
         |}
         |""".stripMargin

    implicit val formats: Formats = DefaultFormats
    val request = Extraction.decompose(Map(
      "spec" → Map(
        "template" → Map(
          "spec" → dialect.filterNot { case (k, _) ⇒ k == "containers" }
        )
      )
    )) merge Extraction.decompose(read[Any](base))
    write(request)
  }
}

case class KubernetesApiResponse(items: List[KubernetesItem] = Nil)

case class KubernetesItem(metadata: KubernetesMetadata, spec: KubernetesSpec, status: KubernetesStatus)

case class KubernetesMetadata(name: String, labels: Map[String, String] = Map())

case class KubernetesSpec(replicas: Option[Int] = None, template: Option[KubernetesTemplate] = None, ports: List[KubernetesPort] = Nil, clusterIP: Option[String] = None)

case class KubernetesPort(name: String, protocol: String, port: Int, nodePort: Int)

case class KubernetesTemplate(spec: KubernetesTemplateSpec)

case class KubernetesTemplateSpec(containers: List[KubernetesContainer] = Nil)

case class KubernetesContainer(resources: KubernetesContainerResource, ports: List[KubernetesContainerPort] = Nil)

case class KubernetesContainerPort(containerPort: Int)

case class KubernetesContainerResource(requests: KubernetesContainerResourceRequests)

case class KubernetesContainerResourceRequests(cpu: String, memory: String)

case class KubernetesStatus(phase: Option[String] = None, podIP: Option[String] = None)
