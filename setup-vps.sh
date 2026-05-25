#!/bin/bash
set -e

echo "=========================================================="
echo " Starting Codebase Assistant VPS Setup Script "
echo "=========================================================="

# Check if script is run as root
if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (use sudo)."
  exit 1
fi

# 1. Update package index and install prerequisites
echo "--> Updating package index..."
apt-get update -y -q
apt-get install -y -q curl git apt-transport-https ca-certificates gnupg lsb-release

# 2. Install Docker if not present
if ! command -v docker &> /dev/null; then
  echo "--> Installing Docker..."
  mkdir -p /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg --yes
  
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
    
  apt-get update -y -q
  apt-get install -y -q docker-ce docker-ce-cli containerd.io docker-compose-plugin
  echo "--> Docker installed successfully!"
else
  echo "--> Docker is already installed."
fi

# 3. Create .env file if it does not exist
if [ ! -f .env ]; then
  echo "--> Creating .env file..."
  
  # Prompt for OpenAI API Key
  echo -n "Enter your OpenAI API Key (sk-...): "
  read -r api_key
  
  # Generate secure random DB password and JWT secret
  db_pass=$(openssl rand -hex 16)
  jwt_sec=$(openssl rand -hex 32)
  
  cat <<EOT > .env
DB_PASSWORD=$db_pass
JWT_SECRET=$jwt_sec
OPENAI_API_KEY=$api_key
EOT
  echo "--> Generated .env file successfully with secure passwords!"
else
  echo "--> .env file already exists. Skipping creation."
fi

# 4. Start the application
echo "--> Building and starting containers with Docker Compose..."
docker compose -f docker-compose.prod.yml up -d --build

echo "=========================================================="
echo " Deployment Started Successfully!"
echo " Visit http://<YOUR_VPS_IP> in your browser to test it."
echo "=========================================================="
