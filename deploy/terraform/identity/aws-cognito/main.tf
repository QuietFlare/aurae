# A Cognito user pool to test that Aurae works with an identity provider other than Keycloak.
# Test users are not created here: users are data, and their passwords would end up in the state
# file. create-test-users.sh adds them.

resource "aws_cognito_user_pool" "aurae_test" {
  name                     = "aurae-test"
  auto_verified_attributes = ["email"]
  mfa_configuration        = "OPTIONAL"

  # Nobody can sign up on their own. An admin creates every account.
  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  username_configuration {
    case_sensitive = false
  }

  software_token_mfa_configuration {
    enabled = true
  }

  password_policy {
    minimum_length    = 12
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = true
  }
}

resource "aws_cognito_user_group" "role" {
  for_each     = var.groups
  name         = each.value
  user_pool_id = aws_cognito_user_pool.aurae_test.id
}

# Domain names are unique across all of AWS, hence the random suffix.
resource "random_id" "domain_suffix" {
  byte_length = 3
}

# Cognito's hosted sign-in page. Version 2 is "managed login". Only this version honours
# prompt=login, which Aurae sends so that shared practice PCs always ask for the password.
resource "aws_cognito_user_pool_domain" "sign_in" {
  domain                = "aurae-test-${random_id.domain_suffix.hex}"
  user_pool_id          = aws_cognito_user_pool.aurae_test.id
  managed_login_version = 2
}

resource "aws_cognito_user_pool_client" "aurae_web" {
  name         = "aurae-web"
  user_pool_id = aws_cognito_user_pool.aurae_test.id

  # Aurae signs in on the server side, so it can keep a secret.
  generate_secret                      = true
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["openid", "profile", "email"]
  supported_identity_providers         = ["COGNITO"]
  explicit_auth_flows                  = ["ALLOW_REFRESH_TOKEN_AUTH"]
  prevent_user_existence_errors        = "ENABLED"

  callback_urls = [for url in var.app_urls : "${url}/login/oauth2/code/oidc"]
  logout_urls   = [for url in var.app_urls : "${url}/"]
}

# Managed login needs a style per app client. Cognito's own default look is enough for testing.
resource "aws_cognito_managed_login_branding" "aurae_web" {
  user_pool_id                = aws_cognito_user_pool.aurae_test.id
  client_id                   = aws_cognito_user_pool_client.aurae_web.id
  use_cognito_provided_values = true
}

# Aurae's settings for this provider. Both files hold the client secret, and Git ignores both.
locals {
  aurae_oidc = {
    issuer_uri    = "https://cognito-idp.${var.region}.amazonaws.com/${aws_cognito_user_pool.aurae_test.id}"
    client_id     = aws_cognito_user_pool_client.aurae_web.id
    client_secret = aws_cognito_user_pool_client.aurae_web.client_secret
    role_claim    = "cognito:groups"
    # Spring reads this from the user-info response. Cognito calls it "username" there,
    # and "cognito:username" only in the ID token.
    user_name_attribute = "username"
    # Cognito's sign-out takes client_id and logout_uri, not the standard OpenID Connect parameters.
    logout_url = "https://${aws_cognito_user_pool_domain.sign_in.domain}.auth.${var.region}.amazoncognito.com/logout?client_id=${aws_cognito_user_pool_client.aurae_web.id}&logout_uri={postLogoutUri}"
  }
}

# For bootRun: Spring loads api/config/application-cognito.yaml when the cognito profile is active.
resource "local_sensitive_file" "aurae_settings" {
  filename        = "${path.module}/../../../../api/config/application-cognito.yaml"
  file_permission = "0600"
  content = yamlencode({
    aurae = {
      oidc = {
        issuer-uri          = local.aurae_oidc.issuer_uri
        client-id           = local.aurae_oidc.client_id
        client-secret       = local.aurae_oidc.client_secret
        user-name-attribute = local.aurae_oidc.user_name_attribute
      }
      security = {
        role-claim = local.aurae_oidc.role_claim
        logout-url = local.aurae_oidc.logout_url
      }
    }
  })
}

# For the deploy Compose stack: docker compose --env-file .env --env-file .env.cognito up
resource "local_sensitive_file" "compose_env" {
  filename        = "${path.module}/../../../compose/.env.cognito"
  file_permission = "0600"
  content         = <<-EOT
    AURAE_OIDC_ISSUER_URI=${local.aurae_oidc.issuer_uri}
    AURAE_OIDC_CLIENT_ID=${local.aurae_oidc.client_id}
    AURAE_OIDC_CLIENT_SECRET=${local.aurae_oidc.client_secret}
    AURAE_OIDC_USER_NAME_ATTRIBUTE=${local.aurae_oidc.user_name_attribute}
    AURAE_SECURITY_ROLE_CLAIM=${local.aurae_oidc.role_claim}
    AURAE_SECURITY_LOGOUT_URL=${local.aurae_oidc.logout_url}
  EOT
}
