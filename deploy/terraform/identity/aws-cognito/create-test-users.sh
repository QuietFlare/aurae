#!/usr/bin/env bash
# Creates one test user per group in the Cognito test pool. Passwords are random and saved to
# api/config/cognito-test-users.txt, which Git ignores. Safe to run again: existing users are skipped.
set -euo pipefail

cd "$(dirname "$0")"
pool_id=$(terraform output -raw user_pool_id)
# Use the region Terraform used, whatever the active AWS profile says. Assigned before the export,
# because "export X=$(...)" hides a failing command from set -e.
AWS_REGION=$(terraform output -raw region)
export AWS_REGION
out=../../../../api/config/cognito-test-users.txt
mkdir -p "$(dirname "$out")" && touch "$out" && chmod 600 "$out"

for group in mfa gp admin; do
  user="$group.test"
  if aws cognito-idp admin-get-user --user-pool-id "$pool_id" --username "$user" > /dev/null 2>&1; then
    echo "$user exists, skipped"
    continue
  fi
  password="$(openssl rand -base64 12)Aa1!"
  aws cognito-idp admin-create-user --user-pool-id "$pool_id" --username "$user" \
    --user-attributes Name=email,Value="$user@example.invalid" Name=email_verified,Value=true \
    --message-action SUPPRESS > /dev/null
  aws cognito-idp admin-set-user-password --user-pool-id "$pool_id" --username "$user" \
    --password "$password" --permanent
  aws cognito-idp admin-add-user-to-group --user-pool-id "$pool_id" --username "$user" --group-name "$group"
  echo "$user $password" >> "$out"
  echo "$user created"
done
