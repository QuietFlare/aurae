# Aurae

Self-hosted lab results platform for GP practices. Aurae reads lab reports the practice did not order, from hospitals, specialists and labs abroad, arriving on paper, by fax or as PDFs. It turns them into structured values with LOINC codes, units and reference ranges and writes them into the practice software as LDT, so they sit next to the results the practice ordered. All data stays on the practice's own server.

## Status

Early development. Aurae is not ready for use with real patient data.

## What Aurae does not do

Aurae organises and displays lab data. It shows whether a value lies inside or outside the reference range of the lab that measured it. It does not interpret results, diagnose or recommend treatment. Those decisions stay with the clinician.

## Stack

React With Typescript on Vite, Kotlin, Spring Boot, PostgreSQL, Docker, Keycloak and Kubernetes (k3s).

The application builds to one container image. That image runs unchanged under Docker Compose for development and under k3s for a practice. All configuration comes in through environment variables, and Spring Actuator provides the health endpoints k3s uses as probes. The two environments differ only on the platform side: secrets, TLS and ingress, and volumes. A practice that cannot run k3s runs the same image with Compose.

## License

Aurae is licensed under the GNU AGPL v3.0. See [LICENSE](LICENSE).
