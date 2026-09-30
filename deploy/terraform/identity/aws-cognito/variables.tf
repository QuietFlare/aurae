variable "region" {
  description = "AWS region for the user pool. Frankfurt keeps identity data in the EU."
  type        = string
  default     = "eu-central-1"
}

variable "app_urls" {
  description = "Addresses Aurae runs on. Cognito only sends users back to these."
  type        = list(string)
  default     = ["http://localhost:8080", "http://localhost:5173", "https://aurae.localhost"]
}

variable "groups" {
  description = "Cognito groups. They arrive in the token as cognito:groups and map to Aurae roles."
  type        = set(string)
  default     = ["mfa", "gp", "admin"]
}
