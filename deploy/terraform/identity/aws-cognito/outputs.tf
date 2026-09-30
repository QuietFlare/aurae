output "region" {
  value = var.region
}

output "user_pool_id" {
  value = aws_cognito_user_pool.aurae_test.id
}

output "issuer_uri" {
  value = "https://cognito-idp.${var.region}.amazonaws.com/${aws_cognito_user_pool.aurae_test.id}"
}

output "client_id" {
  value = aws_cognito_user_pool_client.aurae_web.id
}

output "sign_in_domain" {
  value = "https://${aws_cognito_user_pool_domain.sign_in.domain}.auth.${var.region}.amazoncognito.com"
}
